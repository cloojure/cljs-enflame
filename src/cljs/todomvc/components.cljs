(ns todomvc.components
  "These functions are all Reagent components"
  (:require
    [todomvc.react-bootstrap-core :as rbs]
    ;[cljs-react-bootstrap.react-bootstrap.dropdown :as rbs-dropdown]
    ;[cljs-react-bootstrap.react-bootstrap.modal :as rbs-modal]
    ;[cljs-react-bootstrap.react-bootstrap.navbar :as rbs-navbar]

    [goog.string :as gstring]
    [clojure.string :as str]
    [oops.core :as oops]
    [reagent.core :as r]
    [todomvc.enflame :as flame]
    [tupelo.core :as t]
    [tupelo.char :as char]
    [tupelo.string :as ts]
    ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(def nbsp (gstring/unescapeEntities "&nbsp;")) ; get a char we can use in hiccup   ; #todo => tupelo

(defn input-field
  [{:keys [title on-save on-stop]}] ; #todo -> (with-map-vals [title on-save on-stop] ...)
  (let [text-val (r/atom title) ; local state
        stop-fn  (fn []
                   (reset! text-val "")
                   (when on-stop (on-stop)))
        save-fn  (fn []
                   (on-save (-> @text-val str str/trim))
                   (stop-fn))]
    (fn [props]
      [:input
       (merge (dissoc props :on-save :on-stop :title)
         {:type        "text"
          :value       @text-val
          :auto-focus  true
          :on-blur     save-fn
          :on-change   #(reset! text-val (flame/event-value %))
          :on-key-down #(let [rcvd (.-which %)] ; KeyboardEvent property
                          (condp = rcvd
                            char/code-point-return (save-fn)
                            char/code-point-escape (stop-fn)
                            nil))})])))

(defn task-list-row []
  (let [editing (r/atom false)]
    (fn [{:keys [id completed title]}]
      [:li {:class (cond-> ""
                     completed (str " completed")
                     @editing (str " editing"))}
       [:div.view
        [:input.toggle
         {:type      :checkbox
          :checked   completed
          :on-change #(flame/dispatch-event [:toggle-completed id])}]
        [:label
         {:on-double-click #(reset! editing true)}
         title]
        [:button.destroy
         {:on-click #(flame/dispatch-event [:delete-todo id])}]]
       (when @editing
         [input-field
          {:class   "edit"
           :title   title
           :on-save #(if (seq %)
                       (flame/dispatch-event [:update-title id %])
                       (flame/dispatch-event [:delete-todo id]))
           :on-stop #(reset! editing false)}])])))

(defn task-list []
  (let [visible-todos (flame/reactive-value [:visible-todos :a :b])
        all-complete? (flame/reactive-value [:all-complete?])]
    [:section#main
     [:input#toggle-all
      {:type      "checkbox"
       :checked   all-complete?
       :on-change #(flame/dispatch-event [:complete-all-toggle])}]
     [:label        ; #todo this does not seem to work (as a tooltip?)
      {:for "toggle-all"}
      "Mark all as complete"]
     [:ul#todo-list
      (for [todo-curr visible-todos]
        ^{:key (:id todo-curr)} [task-list-row todo-curr])]])) ; delegate to task-list-row component

; These buttons will dispatch events that will cause browser navigation observed by History
; and propagated via secretary.
(defn footer-controls []
  (let [[num-active num-completed] (flame/reactive-value [:footer-counts 1 2])
        display-mode (flame/reactive-value [:display-mode])]
    [:footer#footer
     [:span#todo-count
      [:strong num-active]
      (ts/pluralize-with num-active " item") " left  (" display-mode ")"]
     [rbs/button-group
      ;[rbs/button {:active false :onClick #(println "*****  BUTTON CLICKED!!!  ***** ") } "Hello Bootstrap!"]
      [rbs/button { :id      :all ; :bs-style "primary"
                   :active true
                   :on-click #(flame/dispatch-event [:set-display-mode :all])} "All"]
      [rbs/button { :id      :active
                   :on-click #(flame/dispatch-event [:set-display-mode :active])} "Active"]
      [rbs/button { :id      :completed
                   :on-click #(flame/dispatch-event [:set-display-mode :completed])} "Completed"]
      (when (pos? num-completed)
        [rbs/button {; :id      :completed
                     :on-click #(flame/dispatch-event [:clear-completed])} "Clear Completed"])
      ]
     ;(when (pos? num-completed)
     ;  [:button#clear-completed
     ;   {:on-click #(flame/dispatch-event [:clear-completed])}
     ;   "Clear Completed"])
     ]))

(defn task-entry []
  [:header#header
   [:h1 "todos"]
   [input-field
    {:id          "new-todo"
     :placeholder "What needs to be done?"
     :on-save     #(when (t/not-empty? (str/trim %))
                     (flame/dispatch-event [:add-todo %]))}]])

(defn todo-root []
  [:div
   [:section#todoapp
    [task-entry]
    (when (t/not-empty? (flame/reactive-value [:todos]))
      [task-list])
    [footer-controls]]
   [:footer#info
    [:p "Double-click to edit a todo"]]])

;---------------------------------------------------------------------------------------------------

(defn ajax-says []
  [:div
   [:span {:style {:color :darkgreen}} [:strong "AJAX says: "]]
   [:span {:style {:font-style :italic}} nbsp nbsp (flame/reactive-value [:ajax-response])]])

(defn root []       ; was simple-component
  [:div
   [:hr]
   [:div
    [:p "I am a component!"]
    [:p.someclass
     "I have " [:strong "bold"]
     [:span {:style {:color "red"}} " and red"] " text."]]
   [:hr]
   [ajax-says]
   [:hr]
   [:div
    [todo-root]]
   [:hr] ])

