(ns todomvc.events
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :as rf]
    [re-frame.std-interceptors :as rfstd]
    [todomvc.db :as todo-db]
    [todomvc.enflame :as flame]
  ))

(enable-console-print!)

; context map (ctx):   { :coeffects   {:db {...}
;                                      :other {...}}
;                        :effects     {:db {...}
;                                      :dispatch [...]}
;                        ... ; other stuff }
; interceptors' :before fns should accumulate data into :coeffects map
; interceptors' :after  fns should accumulate data into   :effects map

;-----------------------------------------------------------------------------
; #todo unify interceptors/handlers:  all accept & return
; #todo   ctx => {:state {...}   ; was `:db`
; #todo           ... ...}       ; other info here
; #todo unify coeffects (input vals) and effects (output vals)
; #todo interceptors must fill in or act up vals (keys) they care about

; #todo (definterceptor my-intc  ; added to :id field as kw
; #todo   "doc string"
; #todo   {:enter (fn [ctx] ...)              tx: ctx => ctx
; #todo    :leave (fn [ctx] ...) } )

;-----------------------------------------------------------------------------
; #todo :before    => :enter       to match pedestal
; #todo :after     => :leave

; coeffects  =>  state-in
;   effects  =>  state-out

; #todo   maybe rename interceptor chain to intc-chain, proc-chain, transform-chain

; #todo   unify [:dispatch ...] effect handlers
; #todo     {:do-effects [  ; <= always a vector param, else a single effect
; #todo        {:effect/id :eff-tag-1  :par1 1  :par2 2}
; #todo        {:effect/id :eff-tag-2  :effect/delay {:value 200  :unit :ms} ;
; #todo         :some-param "hello"  :another-param :italics } ] }

; #todo make all routes define an intc chain.
; #todo each intc is {:id ...  :enter ...  :leave ...} (coerce if :before/:after found - strictly)
; #todo each :enter/:leave fn is (fn [params-map] ...)
; #todo    where params-map  =>  {:event {:event/id ...  :param1 <val1>  :param2 <val2> ...}
; #todo                           :state {:app          ...
; #todo                                   :local-store  ...
; #todo                                   :datascript   ... }}

; #todo replace (reg-cofx ...)  =>  (definterceptor ...)  ; defines a regular fn

; #todo [:delete-item 42] => {:event/id :delete-item :value 42}
; #todo   {:event/id :add-entry  :key :name :value "Joe"}
; #todo   {:event/id :set-timer  :units :ms :value 50 :action (fn [] (js/alert "Expired!") }

; #todo (dispatch-event {:event/id <some-id> ...} )   => event map
; #todo (add-effect ctx {:effect/id <some-id> ...} )  => updated ctx
; #todo setup, prep, teardown, completion

; -- Interceptors --------------------------------------------------------------
;
; Interceptors are a more advanced topic. So, we're plunging into the deep
; end here.
;
; There is a tutorial on Interceptors in re-frame's `/docs`, but to get
; you going fast, here's a very high level description ...
;
; Every event handler can be "wrapped" in a chain of interceptors. A
; "chain of interceptors" is actually just a "vector of interceptors". Each
; of these interceptors can have a `:before` function and an `:after` function.
; Each interceptor wraps around the "handler", so that its `:before`
; is called before the event handler runs, and its `:after` runs after
; the event handler has run.
;
; Interceptors with a `:before` action, can be used to "inject" values
; into what will become the `coeffects` parameter of an event handler.
; That's a way of giving an event handler access to certain resources,
; like values in LocalStore.
;
; Interceptors with an `:after` action, can, among other things,
; process the effects produced by the event handler. One could
; check if the new value for `app-db` correctly matches a Spec.

; Interceptor which will inject the todos stored in localstore.

(def local-store-todos-intc
  (flame/interceptor-state
    {:id    :local-store-todos-intc
     :enter (fn [state]
              (assoc state ; put the localstore todos into the coeffect under :local-store-todos
                :local-store-todos (into (sorted-map) ; read in todos from localstore, and process into a sorted map
                                     (some->> (.getItem js/localStorage todo-db/js-localstore-key)
                                       (cljs.reader/read-string)))))
     :leave identity}))

; Event handlers change state, that's their job. But what happens if there's
; a bug in the event handler and it corrupts application state in some subtle way?
; First, we create an interceptor called `check-spec-interceptor`. Then,
; we use this interceptor in the interceptor chain of all event handlers.
; When included in the interceptor chain of an event handler, this interceptor
; runs `check-and-throw` `after` the event handler has finished, checking
; the value for `app-db` against a spec.
; If the event handler corrupted the value for `app-db` an exception will be
; thrown. This helps us detect event handler bugs early.
; Because all state is held in `app-db`, we are effectively validating the
; ENTIRE state of the application after each event handler runs.  All of it.
(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-intc
  "Checks app-db for correctness after event handler runs"
  (rf/after ; An `after` interceptor receives `db` from (get-in ctx [:effects db]). Return value is ignored.
    (fn [db -event-]
      (check-and-throw :todomvc.db/db db))))

; Part of the TodoMVC is to store todos in local storage. Here we define an interceptor to do this.
; This interceptor runs `after` an event handler. It stores the current todos into local storage.
(def save-todos-intc
  (rf/after         ; An `after` interceptor receives `db` from (get-in ctx [:effects db]). Return value is ignored.
    todo-db/todos->local-store))

; -- Interceptor Chain ------------------------------------------------------
; Each event handler can have its own chain of interceptors.
; We now create the interceptor chain shared by all event handlers
; which manipulate todos. A chain of interceptors is a vector of interceptors.
; Explanation of the `path` Interceptor is given further below.
(def std-interceptors
  [check-spec-intc
   save-todos-intc
  ;rfstd/debug
  ;flame/trace
  ])

; -- Helpers -----------------------------------------------------------------
(defonce todo-id-atom (atom 0))
(defn next-todo-id
  "Returns the next todo ID in a monolithic sequence. "
  []
  (let [[old -new-] (swap-vals! todo-id-atom inc)]
    old))

; -- Event Handlers ----------------------------------------------------------

; usage:  (flame/dispatch-event [:initialise-db])
;
; This event is dispatched when the app's `main` ns is loaded (todomvc.core).
; It establishes initial application state in `app-db`. That means merging:
;   1. Any todos stored in LocalStore (from the last session of this app)
;   2. Default initial values
(defn initialise-db-handler [ state  -event- ] ; note that `state` is coeffects/effects (ignore the difference)
    (js/console.log :initialise-db :enter state )
    (let [{:keys [db local-store-todos]} state
          result {:db todo-db/default-db ; #awt
                  ; #awt (assoc todo-db/default-db :todos local-store-todos)
                 }]
      (js/console.log :initialise-db :leave result)
      result))

; #todo   => (event-handler-set!    :evt-name  (fn [& args] ...)) or subscribe-to  subscribe-to-event
; #todo   => (event-handler-clear!  :evt-name)
; #todo option for log wrapper (with-event-logging  logger-fn
; #todo                          (event-handler-set! :evt-name  (fn [& args] ...)))
; #todo  context -> event
; #todo    :event/id
; #todo    :event/params
; #todo    coeffects -> inputs    ; data
; #todo      effects -> outputs   ; result

; Need a way to document event names and args
;    #todo (defevent set-showing [state])
; #todo event handlers take only params-map (fn [params :- tsk/Map] ...)

; usage:  (flame/dispatch-event [:set-showing :active])
; This event is dispatched when the user clicks on one of the 3 filter buttons at the bottom of the display.
; #todo #awt merge => global state (old cofx)

; #TODO CHANGE ALL EVENTS to be maps => {:id :set-showing   :new-filter-kw :completed ...}
; #TODO CHANGE ALL HANDLERS to be (defn some-handler [state event]   (with-map-vals event [id new-filter-kw] ...)

(defn set-showing-handler
  [state [-e- new-filter-kw]]     ; new-filter-kw is one of :all, :active or :done
    (assoc-in state [:db :showing] new-filter-kw))

(defn add-todo-handler
  [state [-e- text]] ; => {:global-state xxx   :event {:event-name xxx  :arg1 yyy  :arg2 zzz ...}}
    (update-in state [:db :todos]  ; #todo make this be (with-path state [:db :todos] ...) macro
      (fn [todos]                 ; #todo kill this part
        (let [new-id (next-todo-id)
              result (assoc-in todos [new-id] {:id new-id :title text :done false})]
          (js/console.info :add-todo :leave result)
          result))))

(defn toggle-done-handler
  [state [-e- todo-id]]
    (update-in state [:db :todos]
      (fn [todos]
        (let [result (update-in todos [todo-id :done] not)]
          (js/console.info :toggle-done :leave result)
          result))))

(defn save-handler
  [state [-e- todo-id title]]
    (let [result (assoc-in state [:db :todos todo-id :title] title)]
      (js/console.info :save :leave result )
      result))

(defn delete-todo-handler
  [state [-e- todo-id]]
    (let [result (flame/dissoc-in state [:db :todos todo-id])]
      (js/console.info :delete-todo :leave result )
      result))

(defn clear-completed-handler
  [state -event-]
    (let [todos     (get-in state [:db :todos])
          done-ids  (->> (vals todos) ; find id's for todos where (:done -> true)
                      (filter :done)
                      (map :id))
          todos-new (reduce dissoc todos done-ids) ; delete todos which are done
          result    (assoc-in state [:db :todos] todos-new)]
      (js/console.info :clear-completed :leave result)
      result))

(defn complete-all-toggle-handler
  [state -event-]
    (let [todos     (get-in state [:db :todos])
          new-done  (not-every? :done (vals todos)) ; work out: toggle true or false?
          todos-new (reduce #(assoc-in %1 [%2 :done] new-done)
                      todos
                      (keys todos))
          result    (assoc-in state [:db :todos] todos-new)]
      (js/console.info :complete-all-toggle :leave result)
      result))

(defn register-handlers! []
  (flame/event-handler-for! :initialise-db
    [local-store-todos-intc check-spec-intc]
    initialise-db-handler)

  (flame/event-handler-for! :set-showing ; receives events from URL changes via History/secretary
    [check-spec-intc]
    set-showing-handler)

  (flame/event-handler-for! :add-todo
    std-interceptors
    add-todo-handler)

  (flame/event-handler-for! :toggle-done
    std-interceptors
    toggle-done-handler)

  (flame/event-handler-for! :save
    std-interceptors
    save-handler)

  (flame/event-handler-for! :delete-todo
    std-interceptors
    delete-todo-handler)

  (flame/event-handler-for! :clear-completed
    std-interceptors
    clear-completed-handler)

  (flame/event-handler-for! :complete-all-toggle
    std-interceptors
    complete-all-toggle-handler))
