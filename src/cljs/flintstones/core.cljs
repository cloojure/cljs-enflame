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
    [schema.core :as s])
  (:import [goog.history EventType ; Html5History
           ]))

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

; #todo need plumatic schema and tsk/KeyMap
(def display-filter-modes #{:all :active :completed})

; Saves current 'showing' mode (3 filter buttons at the bottom of the display)
(s/defn browser-nav-handler
  [new-filter-kw :- s/Keyword] ; :- #{ :all, :active or :completed }
  (let [filter-kw (t/cond-it-> new-filter-kw
                    (not (contains? display-filter-modes it)) :all)]
    (t/spyx :browser-nav-handler filter-kw)
    (flame/fire-event [:set-display-mode filter-kw]) ))

; NOTE:  accountant (with secretary) only prevents page loads when the user clicks a hyperlink on the page
; (or the code simulates this with a (accountant/navigate! ...) call). If the user types an address in
; the browser navigation bar (or does a RELOAD), the browser WILL FETCH THE PAGE from the
; server even if the route matches a secretary/defroute entry.
;
; In this event, the server needs either an explicit route or a catchall route to return the original index.html
; (or whatever) page containing the SPA code, else the server will return a 404
(defn configure-browser-routing! []
  (println "configure-browser-routing - enter")

  (secretary/defroute "/all" []
    (println :secretary-route-all)
    (browser-nav-handler :all))
  (secretary/defroute "/active" []
    (println :secretary-route-active)
    (browser-nav-handler :active))
  (secretary/defroute "/completed" []
    (println :secretary-route-completed)
    (browser-nav-handler :completed))
  (secretary/defroute "/*" []
    (println ":secretary-route-all  *** default ***")
    (browser-nav-handler :all))

  (accountant/configure-navigation!
    {:nav-handler  (fn [path]
                     (t/spy :accountant--nav-handler--path path)
                     (secretary/dispatch! path))
     :path-exists? (fn [path]
                     (t/spy :accountant--path-exists?--path path)
                     (t/spy :accountant--path-exists?--result
                       (secretary/locate-route path)))})
  (println "configure-browser-routing - leave"))

;---------------------------------------------------------------------------------------------------
(defn app-start
  "Initiates the cljs application"
  []
  (println "app-start - enter")
  (events/define-all-events!)
  (flames/initialize-all)
  (configure-browser-routing!)

  ; Put an initial value into :app-state. The event handler for `:initialize-app-state` can be found in `events.cljs`
  ; Using the sync version of dispatch means that value is in place before we go onto the next step.
  (flame/fire-event-sync [:initialize-app-state])
  (newline)
  (println "initial path to /all - before")
  (accountant/navigate! (str "/all" )) ; simulate user going to URL path `/all`
  (println "initial path to /all - after")

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

