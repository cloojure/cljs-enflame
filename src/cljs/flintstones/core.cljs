(ns flintstones.core
  (:require ; [bidi.bidi :as bidi]
    [accountant.core :as accountant]
    [devtools.core :as devtools]
    [flintstones.slate :as slate]
    [goog.events]
    [reagent.core :as r]
    [secretary.core :as secretary]
    [todomvc.components :as components]
    [todomvc.enflame :as flame]
    [todomvc.events :as events] ; These two are only required to make the compiler
    [todomvc.flames :as flames] ; load them (see docs/Basic-App-Structure.md)
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
  (flame/fire-event [:ajax-response response]) )

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
  (events/define-all-events!)
  (flames/initialize)

  (println "accountant/configure-navigation! - before")
  (accountant/configure-navigation!
    {:nav-handler  (fn [path]
                     (secretary/dispatch! path))
     :path-exists? (fn [path]
                     (secretary/locate-route path)) })
  (println "accountant/configure-navigation! - after")

  ; Put an initial value into :app-state. The event handler for `:initialize-app-state` can be found in `events.cljs`
  ; Using the sync version of dispatch means that value is in place before we go onto the next step.
  (flame/fire-event-sync [:initialize-app-state])
  (flame/fire-event [:set-display-mode :all])
  ; #todo remove this - make a built-in :init that every event-handler verifies & waits for (top priority)
  ; #todo add concept of priority to event dispatch

  (flame/fire-event [:ajax-demo :get "/fox.txt"
                         {:handler       ajax-handler
                          :error-handler ajax-error-handler
                          :headers       {"custom" "something"}
                          }])


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

