(ns todomvc.events
  (:require
    [cljs.spec.alpha :as s]
    [re-frame.core :as rf]
    [re-frame.std-interceptors :as rfstd]
    [todomvc.db :as todo-db]
    [todomvc.enflame :as flame]
  ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; context map (ctx) =>   { :coeffects   {:db {...}
;                                        :other {...}}
;                          :effects     {:db {...}
;                                        :dispatch [...]}
;                          ... ; other stuff }
; interceptors' :before fns should accumulate data into :coeffects map
; interceptors' :after  fns should process    data from   :effects map

;-----------------------------------------------------------------------------
; #todo (definterceptor my-intc  ; added to :id field as kw
; #todo   "doc string"
; #todo   {:enter (fn [state] ...)       to match pedestal
; #todo    :leave (fn [state] ...) } )
; #todo event handlers: document that `state` is coeffects/effects (ignore the difference)
;     coeffects  =>  state-in
;       effects  =>  state-out

; #todo   maybe rename interceptor chain to intc-chain, proc-chain, transform-chain

; #todo   unify [:dispatch ...] effect handlers
; #todo     {:do-effects [  ; <= always a vector param, else a single effect
; #todo        {:effect/id :eff-tag-1  :par1 1  :par2 2}
; #todo        {:effect/id :eff-tag-2  :effect/delay {:value 200  :unit :ms} ;
; #todo         :some-param "hello"  :another-param :italics } ] }

; #todo make all routes define an intc chain.

; #todo [:delete-item 42] => {:event/id :delete-item :idx 42}
; #todo   {:event/id :set-timer  :units :ms :value 50 :action (fn [] (js/alert "Expired!") }

; #todo (dispatch-event {:event/id <some-id> ...} )   => event map
; #todo (add-task state {:effect/id <some-id> ...} )  => updated state

; #todo setup, prep, resources, augments, ancillary, annex, ctx, info, data
; #todo environment, adornments, supplements

; #todo teardown, completion, tasks, commands, orders

(def local-store-todos-intc ; injects the todos stored in localstore.
  (flame/interceptor-state
    {:id    :local-store-todos-intc
     :enter (fn [state]
              (assoc state ; put the localstore todos into the coeffect under :local-store-todos
                :local-store-todos (into (sorted-map) ; read in todos from localstore, and process into a sorted map
                                     (some->> (.getItem js/localStorage todo-db/js-localstore-key)
                                       (cljs.reader/read-string)))))
     :leave identity}))

(def check-spec-intc
  "Checks app-db for correctness after event handler runs"
  (rf/after ; An `after` interceptor receives `db` from (get-in ctx [:effects db]). Return value is ignored.
    (fn [db -event-]
      (when-not (s/valid? :todomvc.db/db db)
        (throw (ex-info (str "spec check failed: " (s/explain-str :todomvc.db/db db)) db))))))

; Part of the TodoMVC Challenge is to store todos in local storage. Here we define an interceptor to do this.
; This interceptor runs `after` an event handler. It stores the current todos into local storage.
(def save-todos-intc
  (rf/after ; An `after` interceptor receives `db` from (get-in ctx [:effects db]). Return value is ignored.
    todo-db/todos->local-store))

(def common-interceptors
  [check-spec-intc
   save-todos-intc
  ;rfstd/debug
  ;flame/trace
   flame/trace-print
  ])

(defonce todo-id-atom (atom 0))
(defn next-todo-id
  "Returns the next todo ID in a monolithic sequence. "
  []
  (flame/swap-out! todo-id-atom inc))

; This event is dispatched when the app's `main` ns is loaded (todomvc.core).
; It establishes initial application state in `app-db`. That means merging:
;   1. Any todos stored in LocalStore (from the last session of this app)
;   2. Default initial values
(defn initialise-db-handler [state  -event-]
    (js/console.log :initialize-db :enter state )
    (let [{:keys [db local-store-todos]} state
          result {:db todo-db/default-db ; #awt
                  ; #awt (assoc todo-db/default-db :todos local-store-todos)
                 }]
      (js/console.log :initialize-db :leave result)
      result))

; Need a way to document event names and args
;    #todo (defevent set-showing [state])
; #todo event handlers take only params-map (fn [params :- tsk/Map] ...)

; #todo #awt merge => global state (old cofx)

; #TODO CHANGE ALL EVENTS to be maps => {:id :set-showing   :new-filter-kw :completed ...}
; #TODO CHANGE ALL HANDLERS to be (defn some-handler [state event]   (with-map-vals event [id new-filter-kw] ...)

(defn set-showing-handler
  "Handles clicks on one of the 3 filter buttons at the bottom of the display."
  [state [-e-
          new-filter-kw]] ; :- #{ :all, :active or :done }
  (assoc-in state [:db :showing] new-filter-kw))

(defn add-todo-handler [state [-e- text]]
  (update-in state [:db :todos] ; #todo make this be (with-path state [:db :todos] ...) macro
    (fn [todos]     ; #todo kill this part
      (let [new-id (next-todo-id)]
        (into todos {new-id {:id new-id :title text :done false}})))))

(defn toggle-done-handler [state [-e- todo-id]]
  (update-in state [:db :todos]
    (fn [todos] (update-in todos [todo-id :done] not))))

(defn save-handler [state [-e- todo-id title]]
  (assoc-in state [:db :todos todo-id :title] title))

(defn delete-todo-handler [state [-e- todo-id]]
  (flame/dissoc-in state [:db :todos todo-id]))

(defn clear-completed-handler
  [state -event-]
  (let [todos     (get-in state [:db :todos])
        done-ids  (->> (vals todos) ; find id's for todos where (:done -> true)
                    (filter :done)
                    (map :id))
        todos-new (reduce dissoc todos done-ids) ; delete todos which are done
        result    (assoc-in state [:db :todos] todos-new)]
    result))

(defn complete-all-toggle-handler
  [state -event-]
  (let [todos     (get-in state [:db :todos])
        new-done  (not-every? :done (vals todos)) ; work out: toggle true or false?
        todos-new (reduce #(assoc-in %1 [%2 :done] new-done)
                    todos
                    (keys todos))
        result    (assoc-in state [:db :todos] todos-new)]
    result))

(defn register-handlers! []
  (flame/event-handler-for! :initialize-db   ; usage: (flame/dispatch-event [:initialise-db])
    [local-store-todos-intc check-spec-intc]
    initialise-db-handler)

  (flame/event-handler-for! :set-showing ; receives events from URL changes via History/secretary
    [check-spec-intc]
    set-showing-handler)

  (flame/event-handler-for! :add-todo
    common-interceptors
    add-todo-handler)

  (flame/event-handler-for! :toggle-done
    common-interceptors
    toggle-done-handler)

  (flame/event-handler-for! :save
    common-interceptors
    save-handler)

  (flame/event-handler-for! :delete-todo
    common-interceptors
    delete-todo-handler)

  (flame/event-handler-for! :clear-completed
    common-interceptors
    clear-completed-handler)

  (flame/event-handler-for! :complete-all-toggle
    common-interceptors
    complete-all-toggle-handler))
