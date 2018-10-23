(ns flintstones.core
  (:require
    [devtools.core :as devtools]
    [goog.events :as events]
    [flintstones.slate :as slate]
    [oops.core :as oops]
    [reagent.core :as r]
    [secretary.core :as secretary]
    [todomvc.components]
    [todomvc.enflame :as flame]
    [todomvc.events] ; These two are only required to make the compiler
    [todomvc.topics] ; load them (see docs/Basic-App-Structure.md)
    )
  (:require-macros [secretary.core :as secretary])
  (:import [goog History]
           [goog.history EventType])
  )

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(println
"This text is printed from src/flintstones/core.cljs.
Go ahead and edit it and see reloading in action. Again, or not.")
(println "Hello World! " )

(println "Hello addition:  " (slate/add2 2 3) )

; -- Debugging aids ----------------------------------------------------------
(devtools/install!) ; we love https://github.com/binaryage/cljs-devtools

(defn simple-component []
  [:div
   [:hr]
   [:div
    [:p "I am a component!"]
    [:p.someclass
     "I have " [:strong "bold"]
     [:span {:style {:color "red"}} " and red"] " text."]]
   [:hr]
   [:div
    [todomvc.components/todo-app]]
   [:hr]
   ])

;---------------------------------------------------------------------------------------------------
; Put an initial value into app-db.
; The event handler for `:initialise-db` can be found in `events.cljs`
; Using the sync version of dispatch means that value is in
; place before we go onto the next step.
(flame/dispatch-event-sync [:initialise-db])
; #todo remove this - make a built-in :init that every event-handler verifies & waits for (top priority)
; #todo add concept of priority to event dispatch

;---------------------------------------------------------------------------------------------------
; Set up secretary routing for the event-type filters
(secretary/defroute "/"        []       (flame/dispatch-event [:set-showing :all]))
(secretary/defroute "/:filter" [filter] (flame/dispatch-event [:set-showing (keyword filter)]))
  ; #todo  make an `event` type & factory fn: (event :set-showing :all) instead of bare vec:  [:set-showing :all]
  ; #todo fix secretary (-> bidi?) to avoid dup (:filter x2) and make more like pedestal

(def history
  (doto (History.)
    (events/listen EventType.NAVIGATE
      (fn [event]
        (println :history event)
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;---------------------------------------------------------------------------------------------------
(defonce counter (atom 0))

(defn app-start []
  (r/render [simple-component] (js/document.getElementById "tgt-div")))

(defn figwheel-reload []
  ; optionally touch your app-state to force rerendering depending on your application
  ; (swap! app-state update-in [:__figwheel_counter] inc)
  (println "figwheel-reload/enter => " (swap! counter inc)))

(when (zero? @counter)
  (println "Initial load")
  (figwheel-reload))
(app-start)



