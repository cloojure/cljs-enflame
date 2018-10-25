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
(defn initialise-db-handler [state -event-]
  (js/console.log :initialise-db-handler :enter state)
  (let [local-store-todos (flame/get-in-strict state [:local-store-todos])
        initial-db        (into todo.db/default-db {:todos local-store-todos})
        state-out         (into state {:db initial-db}) ]
    (js/console.log :initialise-db-handler :leave state-out)
    state-out))

(defn set-showing-handler
  "Handles clicks on one of the 3 filter buttons at the bottom of the display."
  [state [-e-
          new-filter-kw]] ; :- #{ :all, :active or :done }
  (assoc-in state [:db :showing] new-filter-kw))

(defn add-todo-handler [state [-e- text]]
  (update-in state [:db :todos] ; #todo make this be (with-path state [:db :todos] ...) macro
    (fn [todos]     ; #todo kill this part
      ; must choose a new id greater than any existing id (possibly from localstore todos)
      (let [todo-ids (keys todos)
            new-id   (if (not-empty todo-ids)
                       (inc (apply max todo-ids))
                       0)]
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
    [todo.db/localstore-load-intc todo.db/check-spec-intc]
    initialise-db-handler)

  (flame/event-handler-for! :set-showing ; receives events from URL changes via History/secretary
    [todo.db/check-spec-intc]
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
