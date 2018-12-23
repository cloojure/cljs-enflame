(ns flintstones.core
  (:require
    ;[cljsjs.react-bootstrap]
    ;[cljs-react-bootstrap.handlers]
    ;[cljs-react-bootstrap.subs]
    ;[cljs-react-bootstrap.views :as views]
    ;[cljs-react-bootstrap.config :as config]
    ;[cljs-react-bootstrap.layout :as layout]

    [devtools.core :as devtools]
    [goog.events]
    [flintstones.slate :as slate]
    [reagent.core :as r]
   ;[bidi.bidi :as bidi]
    [todomvc.components :as components]
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

; #todo  make an `event` type & factory fn: (event :set-showing :all) instead of bare vec:  [:set-showing :all]
; #todo fix secretary (-> bidi?) to avoid dup (:filter x2) and make more like pedestal

;---------------------------------------------------------------------------------------------------
(defn app-start
  "Initiates the cljs application"
  []
  (println "app-start - enter")
  (events/register-handlers)
  (reactives/initialize)

  ; Put an initial value into :app-state. The event handler for `:initialize-app-state` can be found in `events.cljs`
  ; Using the sync version of dispatch means that value is in place before we go onto the next step.
  (flame/dispatch-event-sync [:initialize-app-state])
  (flame/dispatch-event [:set-display-mode :all])
  ; #todo remove this - make a built-in :init that every event-handler verifies & waits for (top priority)
  ; #todo add concept of priority to event dispatch

  (flame/dispatch-event [:ajax-demo :get "/fox.txt" {:handler       ajax-handler
                                                     :error-handler ajax-error-handler}])


  (r/render [components/root] (js/document.getElementById "tgt-div"))
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

