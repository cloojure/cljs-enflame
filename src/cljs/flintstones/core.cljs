(ns flintstones.core
  (:require
    [devtools.core :as devtools]
    [goog.events]
    [flintstones.slate :as slate]
    [reagent.core :as r]
   ;[secretary.core :as secretary]
   ;[bidi.bidi :as bidi]
    [todomvc.components :as gui]
    [todomvc.enflame :as flame]
    [todomvc.events :as events] ; These two are only required to make the compiler
    [todomvc.reactives :as reactives] ; load them (see docs/Basic-App-Structure.md)
    [tupelo.core :as t]
  )
  (:import [goog.history Html5History EventType]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)
(println
"This text is printed from src/flintstones/core.cljs.
Go ahead and edit it and see reloading in action. Again, or not.")
(println "Hello World! " )
(println "Hello addition:  " (slate/add2 2 3) )
(t/spyx :something (+ 2 3) [1 2 3])

;---------------------------------------------------------------------------------------------------
(defn ajax-handler [response]
  (.log js/console (str "cljs-ajax: successfully read:  " response))
  (flame/dispatch-event [:ajax-response response]) )

(defn ajax-error-handler [{:keys [status status-text]}]
  (.log js/console (str "cljs-ajax: something bad happened:  " status " " status-text)))

; -- Debugging aids ----------------------------------------------------------
(devtools/install!) ; we love https://github.com/binaryage/cljs-devtools

(defn get-token []
  (t/spyx "get-token=" js/window.location.pathname))

(defn make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol "//" js/window.location.host))
    (.setUseFragment false)))

;(defn handle-url-change
;  [event]
;  (js/console.log "handle-url-change event=" event)
;  (js/console.log "handle-url-change navigation=" (.-isNavigation event))
;  (let [token       (get-token) ; (.-token event)
;  ]
;    (js/console.log "handle-url-change token=" token)
;    (secretary/dispatch! token)))

;(defonce history (doto (make-history)
;                   (goog.events/listen EventType.NAVIGATE
;                     #(handle-url-change %)) ; wrap in a fn to allow live reloading
;                   (.setEnabled true)))

; Set up secretary navigation routing for the event-type filters. Must occur before goog.History setup
; since that will fire an event.
;-----------------------------------------------------------------------------
; Sets browser URI to one of
;    /#/all
;    /#/active
;    /#/completed

;(secretary/set-config! :prefix "#")

;(secretary/defroute "/" []
;  (flame/dispatch-event [:set-display-mode :all]))
;
;(secretary/defroute "/:filter" [filter]
;  (let [secr-evt-mode (keyword filter)]
;    (js/console.log :secr-route "filter=" filter)
;    (js/console.log :secr-evt-mode secr-evt-mode)
;    (flame/dispatch-event [:set-display-mode secr-evt-mode])))

;-----------------------------------------------------------------------------
; Here we listen for URL change events and use secretary/dispatch to propagate them to [:set-showing ...]
; Must happend AFTER setting up secretary since `.setEnabled` will fire an event
;(defn setup-history
;  []
;  (doto (History.)
;    (goog.events/listen EventType.NAVIGATE
;      #(handle-url-change %)) ; wrap in a fn to enable live reloading
;    (.setEnabled true))) ; will fire an event

; #todo  make an `event` type & factory fn: (event :set-showing :all) instead of bare vec:  [:set-showing :all]
; #todo fix secretary (-> bidi?) to avoid dup (:filter x2) and make more like pedestal

;---------------------------------------------------------------------------------------------------
(defn app-start
  "Initiates the cljs application"
  []
  (println "app-start - enter")
  (events/register-handlers)
  (reactives/initialize)
 ;(setup-history)

  ; Put an initial value into :app-state. The event handler for `:initialize-app-state` can be found in `events.cljs`
  ; Using the sync version of dispatch means that value is in place before we go onto the next step.
  (flame/dispatch-event-sync [:initialize-app-state])
  (flame/dispatch-event [:set-display-mode :all])
  ; #todo remove this - make a built-in :init that every event-handler verifies & waits for (top priority)
  ; #todo add concept of priority to event dispatch

  (flame/dispatch-event [:ajax-demo :get "/fox.txt" {:handler       ajax-handler
                                                     :error-handler ajax-error-handler}])


  (r/render [gui/root] (js/document.getElementById "tgt-div"))
  (println "app-start - leave")
)

(defonce figwheel-reload-count (atom 0))
(defn figwheel-reload   ; called from project.clj -> :cljsbuild -> :figwheel -> :on-jsload
  []
  (enable-console-print!) ; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
  (swap! figwheel-reload-count inc)
  (println "figwheel-reload/enter => " @figwheel-reload-count))

;***************************************************************************************************
; kick off the app
(app-start)

