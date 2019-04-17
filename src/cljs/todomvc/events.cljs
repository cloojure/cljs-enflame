(ns todomvc.events
  (:require
    [oops.core :as oops]
    [todomvc.app-state :as app-state]
    [todomvc.enflame :as flame]
    [tupelo.core :as t]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; This event is dispatched when the app's `main` ns is loaded (todomvc.core). It establishes
; initial application state in the context map `:app-state` key. That means merging:
;   1. Any todos stored in the browser's LocalStore (from the last session of this app)
;   2. Default initial values
(defn initialise-app-state [ctx -event-]
  (let [local-store-todos (t/grab :local-store-todos ctx)
        initial-state     (t/glue app-state/default-state
                            {:todos local-store-todos})
       ;initial-state     app-state/default-state ; allows user to reset LocalStore during development
        ctx-out           (t/glue ctx {:app-state initial-state})]
    ctx-out))

; #todo need plumatic schema and tsk/KeyMap
(defn set-display-mode
  "Saves current 'showing' mode (3 filter buttons at the bottom of the display)"
  [ctx [-e- new-filter-kw]] ; :- #{ :all, :active or :completed }
  (t/spyx :set-display-mode new-filter-kw)
  (let [all-filter-modes #{:all :active :completed}
        new-filter-kw    (if (contains? all-filter-modes new-filter-kw)
                           new-filter-kw
                           :all)]
    (assoc-in ctx [:app-state :display-mode] new-filter-kw)))

(defn add-todo [ctx [-e- todo-title]]
  (update-in ctx [:app-state :todos] ; #todo make this be (with-path ctx [:app-state :todos] ...) macro
    (fn [todos]     ; #todo kill this part
      ; must choose a new id greater than any existing id (possibly from localstore todos)
      (let [todo-ids (keys todos)
            new-id   (if (t/not-empty? todo-ids)
                       (inc (apply max todo-ids))
                       0)]
        (t/glue todos {new-id {:id new-id :title todo-title :completed false}})))))

(defn toggle-completed [ctx [-e- todo-id]]
  (update-in ctx [:app-state :todos todo-id :completed] not))

(defn update-title [ctx [-e- todo-id todo-title]]
  (assoc-in ctx [:app-state :todos todo-id :title] todo-title))

(defn delete-todo [ctx [-e- todo-id]]
  (t/dissoc-in ctx [:app-state :todos todo-id]))

(defn clear-completed-todos
  [ctx -event-]
  (let [todos         (t/fetch-in ctx [:app-state :todos])
        completed-ids (->> (vals todos) ; find id's for todos where (:completed -> true)
                        (filter :completed)
                        (mapv :id))
        todos-new     (reduce dissoc todos completed-ids) ; delete todos which are completed
        result        (assoc-in ctx [:app-state :todos] todos-new)]
    result))

(defn toggle-completed-all
  "Toggles the completed status for each todo"
  [ctx -event-]
  (let [todos         (t/fetch-in ctx [:app-state :todos])
        new-completed (not-every? :completed (vals todos)) ; work out: toggle true or false?
        todos-new     (reduce #(assoc-in %1 [%2 :completed] new-completed)
                        todos
                        (keys todos))
        result        (assoc-in ctx [:app-state :todos] todos-new)]
    result))

(def common-interceptors
  [app-state/check-spec-intc
   app-state/localstore-save-intc
   flame/ajax-intc
  ;flame/trace-log-intc
   flame/trace-print-intc
  ])

(defn define-all-events!
  "Defines all Enflame events.  Should be called from app main entry point, which
  ensures the event definitions will be evaluated. "
  []
  (flame/define-event
    {:event-id          :initialize-app-state
     :interceptor-chain [app-state/localstore-load-intc app-state/check-spec-intc flame/trace-print-intc]
     :handler-fn        initialise-app-state})

  (flame/define-event
    {:event-id          :set-display-mode ; receives events from URL changes via History/secretary
     :interceptor-chain [app-state/check-spec-intc]
     :handler-fn        set-display-mode})

  (flame/define-event
    {:event-id          :add-todo
     :interceptor-chain common-interceptors
     :handler-fn        add-todo})

  (flame/define-event
    {:event-id          :toggle-completed
     :interceptor-chain common-interceptors
     :handler-fn        toggle-completed})

  (flame/define-event
    {:event-id          :update-title
     :interceptor-chain common-interceptors
     :handler-fn        update-title})

  (flame/define-event
    {:event-id          :delete-todo
     :interceptor-chain common-interceptors
     :handler-fn        delete-todo})

  (flame/define-event
    {:event-id          :clear-completed
     :interceptor-chain common-interceptors
     :handler-fn        clear-completed-todos})

  (flame/define-event
    {:event-id          :complete-all-toggle
     :interceptor-chain common-interceptors
     :handler-fn        toggle-completed-all})

  (flame/define-event
    {:event-id          :ajax-demo
     :interceptor-chain common-interceptors
     :handler-fn        (fn [ctx [-e- method uri opts]]
                          (assoc ctx :ajax (t/glue {:method method :uri uri} opts)))})

  (flame/define-event
    {:event-id          :ajax-response
     :interceptor-chain common-interceptors
     :handler-fn        (fn [ctx [-e- response]]
                          (assoc-in ctx [:app-state :ajax-response] response))})

)








