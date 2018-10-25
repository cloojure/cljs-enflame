(ns todomvc.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as rf]
            [todomvc.enflame :as flame] ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; -- Spec --------------------------------------------------------------------
; This is a clojure.spec specification for the value in app-db. It is like a
; Schema. See: http://clojure.org/guides/spec
;
; The value in app-db should always match this spec. Only event handlers
; can change the value in app-db so, after each event handler
; has run, we re-check app-db for correctness (compliance with the Schema).
;
; How is this completed? Look in events.cljs and you'll notice that all handlers
; have an "after" interceptor which does the spec re-check.
;
; None of this is strictly necessary. It could be omitted. But we find it
; good practice.
(s/def ::id int?)
(s/def ::title string?)
(s/def ::completed boolean?)
(s/def ::todo (s/keys :req-un [::id ::title ::completed]))

(s/def ::todos (s/and ; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::id ::todo) ; in this map, each todo is keyed by its :id
                 #(instance? PersistentTreeMap %))) ; is a sorted-map (not just a map)

(s/def ::showing    ; what todos are shown to the user?
  #{:all            ; all todos are shown
    :active         ; only todos whose :completed is false
    :completed})         ; only todos whose :completed is true
(s/def ::db (s/keys :req-un [::todos ::showing]))

; -- Default app-db Value  ---------------------------------------------------
; When the application first starts, this will be the value put in app-db
; Unless, of course, there are todos in the LocalStore (see further below)
; Look in:
;   1.  `core.cljs` for  "(dispatch-sync [:initialise-db])"
;   2.  `events.cljs` for the registration of :initialise-db handler
(def default-db     ; what gets put into app-db by default.
  {:todos   (sorted-map) ; an empty list of todos. Use the (int) :id as the key
   :showing :all})  ; show all todos

; -- Local Storage  ----------------------------------------------------------
; Part of the todomvc challenge is to store todos in LocalStorage, and
; on app startup, reload the todos from when the program was last run.
; But the challenge stipulates to NOT load the setting for the "showing"
; filter. Just the todos.
(def js-localstore-key "todos-reframe") ; localstore key

(def check-spec-intc
  "Checks app-db for correctness after event handler runs"
  (flame/interceptor
    {:id    :check-spec-intc
     :enter identity
     :leave (fn [state]
              (let [db (flame/get-in-strict state [:db])]
                (when-not (s/valid? :todomvc.db/db db)
                  (println :check-spec-intc :state state)
                  (println :failed-check (s/explain-str :todomvc.db/db db))
                  (throw (ex-info (str "spec check failed: " (s/explain-str :todomvc.db/db db)) db))))
              state)}))

; Part of the TodoMVC Challenge is to store todos in local storage. Here we define an interceptor to do this.
(def localstore-save-intc
  (flame/interceptor
    {:id    :localstore-save-intc
     :enter identity
     :leave (fn [state]
              (let [todos   (flame/get-in-strict state [:db :todos])
                    edn-str (str todos)] ; sorted-map written as an edn string
                (js/console.info :todos->local-store todos)
                (.setItem js/localStorage js-localstore-key edn-str))
              state)}))

 ; read in todos from localstore, and process into a sorted map
(def localstore-load-intc ; injects state with todos from the localstore.
  (flame/interceptor
    {:id    :localstore-load-intc
     :enter (fn [state]
              (let [item-read    (.getItem js/localStorage js-localstore-key)
                    loaded-value (some-> item-read
                                   (cljs.reader/read-string) ; convert edn string => actual map
                                   (flame/->sorted-map)) ; coerce to a sorted map (from unsorted map)
                    state-sort   (into state {:local-store-todos loaded-value})]
                state-sort))
     :leave identity}))

