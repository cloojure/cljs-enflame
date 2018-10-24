(ns flintstones.core
  (:require
    [devtools.core :as devtools]
    [goog.events]
    [flintstones.slate :as slate]
    [oops.core :as oops]
    [reagent.core :as r]
    [secretary.core :as secretary]
    [todomvc.components :as gui]
    [todomvc.enflame :as flame]
    [todomvc.events :as events] ; These two are only required to make the compiler
    [todomvc.topics :as topics] ; load them (see docs/Basic-App-Structure.md)
    )
  (:require-macros [secretary.core :as secretary])
  (:import [goog History]
           [goog.history EventType]))

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
    [gui/root]]
   [:hr] ])

;---------------------------------------------------------------------------------------------------

; Set up secretary navigation routing for the event-type filters
(defn configure-routes! []
  (secretary/defroute "/"        []       (flame/dispatch-event [:set-showing :all]))
  (secretary/defroute "/:filter" [filter] (flame/dispatch-event [:set-showing (keyword filter)])))
; #todo  make an `event` type & factory fn: (event :set-showing :all) instead of bare vec:  [:set-showing :all]
; #todo fix secretary (-> bidi?) to avoid dup (:filter x2) and make more like pedestal
    ; Although we use the secretary library below, that's mostly a historical accident. You might also consider using:
    ;   - https://github.com/DomKM/silk
    ;   - https://github.com/juxt/bidi
    ; We don't have a strong opinion.

; Here we listen for URL change events and use secretary/dispatch to propagate them to [:set-showing ...]
(def history
  (doto (History.)
    (goog.events/listen EventType.NAVIGATE
      (fn [event]
        (println :history event)
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;---------------------------------------------------------------------------------------------------

(defn app-start []
  (configure-routes!)
  (events/register-handlers!)
  (topics/register-topics!)
  ; Put an initial value into app-db. The event handler for `:initialize-db` can be found in `events.cljs`
  ; Using the sync version of dispatch means that value is in place before we go onto the next step.
  (flame/dispatch-event-sync [:initialize-db])
  ; #todo remove this - make a built-in :init that every event-handler verifies & waits for (top priority)
  ; #todo add concept of priority to event dispatch

  (r/render [simple-component] (js/document.getElementById "tgt-div")))

(defonce figwheel-reload-counter (atom 0))
(defn figwheel-reload ; called from project.clj -> :cljsbuild -> :figwheel -> :on-jsload
  []
  (swap! figwheel-reload-counter inc)
  (println "figwheel-reload/enter => " @figwheel-reload-counter))

(app-start) ; ********** kicks off the app **********



