(ns todomvc.events
  (:require
    [todomvc.db :as todo.db]
    [todomvc.enflame :as flame] ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(def common-interceptors
  [todo.db/check-spec-intc
   todo.db/localstore-save-intc
  ;flame/trace
   flame/trace-print
  ])

; This event is dispatched when the app's `main` ns is loaded (todomvc.core).
; It establishes initial application state in `app-db`. That means merging:
;   1. Any todos stored in LocalStore (from the last session of this app)
;   2. Default initial values
(defn initialise-db [ctx -event-]
  (js/console.log :initialise-db-handler :enter ctx)
  (let [local-store-todos (flame/get-in-strict ctx [:local-store-todos])
        initial-db        (into todo.db/default-db {:todos local-store-todos})
        ctx-out         (into ctx {:db initial-db}) ]
    (js/console.log :initialise-db-handler :leave ctx-out)
    ctx-out))

; #todo need plumatic schema and tsk/KeyMap
(defn set-showing-mode
  "Saves current 'showing' mode (3 filter buttons at the bottom of the display)"
  [ctx [-e- new-filter-kw]] ; :- #{ :all, :active or :completed }
  (assoc-in ctx [:db :showing] new-filter-kw))

(defn add-todo [ctx [-e- todo-title]]
  (update-in ctx [:db :todos] ; #todo make this be (with-path ctx [:db :todos] ...) macro
    (fn [todos]     ; #todo kill this part
      ; must choose a new id greater than any existing id (possibly from localstore todos)
      (let [todo-ids (keys todos)
            new-id   (if (not-empty todo-ids)
                       (inc (apply max todo-ids))
                       0)]
        (into todos {new-id {:id new-id :title todo-title :completed false}})))))

(defn toggle-completed [ctx [-e- todo-id]]
  (update-in ctx [:db :todos todo-id :completed] not))

(defn update-title [ctx [-e- todo-id todo-title]]
  (assoc-in ctx [:db :todos todo-id :title] todo-title))

(defn delete-todo [ctx [-e- todo-id]]
  (flame/dissoc-in ctx [:db :todos todo-id]))

(defn clear-completed-todos
  [ctx -event-]
  (let [todos     (get-in ctx [:db :todos])
        completed-ids  (->> (vals todos) ; find id's for todos where (:completed -> true)
                    (filter :completed)
                    (map :id))
        todos-new (reduce dissoc todos completed-ids) ; delete todos which are completed
        result    (assoc-in ctx [:db :todos] todos-new)]
    result))

(defn toggle-completed-all
  "Toggles the completed status for each todo"
  [ctx -event-]
  (let [todos     (get-in ctx [:db :todos])
        new-completed  (not-every? :completed (vals todos)) ; work out: toggle true or false?
        todos-new (reduce #(assoc-in %1 [%2 :completed] new-completed)
                    todos
                    (keys todos))
        result    (assoc-in ctx [:db :todos] todos-new)]
    result))

(defn register-handlers! []
  (flame/event-handler-for! :initialize-db   ; usage: (flame/dispatch-event [:initialise-db])
    [todo.db/localstore-load-intc todo.db/check-spec-intc]
    initialise-db)

  (flame/event-handler-for! :set-showing-mode ; receives events from URL changes via History/secretary
    [todo.db/check-spec-intc]
    set-showing-mode)

  (flame/event-handler-for! :add-todo
    common-interceptors
    add-todo)

  (flame/event-handler-for! :toggle-completed
    common-interceptors
    toggle-completed)

  (flame/event-handler-for! :update-title
    common-interceptors
    update-title)

  (flame/event-handler-for! :delete-todo
    common-interceptors
    delete-todo)

  (flame/event-handler-for! :clear-completed
    common-interceptors
    clear-completed-todos)

  (flame/event-handler-for! :complete-all-toggle
    common-interceptors
    toggle-completed-all))
