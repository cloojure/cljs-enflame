(ns todomvc.db
  (:require [cljs.reader]
            [cljs.spec.alpha :as s]
            [re-frame.core :as rf]))

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
; How is this done? Look in events.cljs and you'll notice that all handlers
; have an "after" interceptor which does the spec re-check.
;
; None of this is strictly necessary. It could be omitted. But we find it
; good practice.
(s/def ::id int?)
(s/def ::title string?)
(s/def ::done boolean?)
(s/def ::todo (s/keys :req-un [::id ::title ::done]))

(s/def ::todos (s/and ; should use the :kind kw to s/map-of (not supported yet)
                 (s/map-of ::id ::todo) ; in this map, each todo is keyed by its :id
                 #(instance? PersistentTreeMap %))) ; is a sorted-map (not just a map)

; (s/def ::todos (s/map-of ::id ::todo)) ; in this map, each todo is keyed by its :id


(s/def ::showing    ; what todos are shown to the user?
  #{:all            ; all todos are shown
    :active         ; only todos whose :done is false
    :done})         ; only todos whose :done is true
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

(defn todos->local-store
  "Puts todos into localStorage"
  [todos]
  (js/console.info :todos->local-store todos)
  (.setItem js/localStorage js-localstore-key (str todos))) ; sorted-map written as an EDN map

