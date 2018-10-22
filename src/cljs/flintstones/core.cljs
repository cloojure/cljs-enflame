(ns flintstones.core
  (:require
    [flintstones.slate :as slate]
    [oops.core :as oops]
    [reagent.core :as r] ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(println
"This text is printed from src/flintstones/core.cljs.
Go ahead and edit it and see reloading in action. Again, or not.")
(println "Hello World! " )

(println "Hello addition:  " (slate/add2 2 3) )

(defn simple-component []
  [:div
   [:p "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red"] " text."]])

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



