(ns todomvc.components
  "These functions are all Reagent components"
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [oops.core :as oops]
    [reagent.core :as r]
    [todomvc.enflame :as flame]
    [tupelo.char :as char]
    [tupelo.core :as t]
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
    (fn [todo-curr]
      (t/spy :task-list-row todo-curr)
      (let [{:keys [id completed title]} todo-curr]
        [:li.list-group-item
         ;{:class (cond->
         ;          "" completed (str " completed")
         ;          @editing (str " editing"))}
         [:div.row
          [:div.form-inline.col-xs-1
           [:input  ; .toggle
            {:type      :checkbox
             :checked   completed
             :on-change #(flame/fire-event [:toggle-completed id])}]]
          [:div.form-group.col-xs-10
           [:label {:on-double-click #(reset! editing true)} title]]
          [:div.col-xs-1
           [:button.btn.btn-xs
            {:on-click #(flame/fire-event [:delete-todo id])
             ;:style    {:float :right}
             } "X"]]
          (when @editing
            [input-field
             {:class   "edit"
              :title   title
              :on-save #(if (seq %)
                          (flame/fire-event [:update-title id %])
                          (flame/fire-event [:delete-todo id]))
              :on-stop #(reset! editing false)}])]]))))

(defn task-list []
  (let [visible-todos (flame/watching [:visible-todos :a :b])
        all-complete? (flame/watching [:all-complete?])]
    [:div.panel-body       ; #main
     [:input#toggle-all
      {:type      "checkbox" :checked   all-complete?
       :on-change #(flame/fire-event [:complete-all-toggle])}]
     [:label {:for "toggle-all"}
      "Mark all as complete"]
     [:ul.list-group           ; #todo-list
      (for [todo-curr visible-todos]
        ^{:key (:id todo-curr)} [task-list-row todo-curr])]])) ; delegate to task-list-row component

; These buttons will dispatch events that will cause browser navigation observed by History
; and propagated via secretary.
(defn footer-controls []
  (let [[num-active num-completed] (flame/watching [:footer-counts 1 2])
        display-mode (flame/watching [:display-mode])]
    [:div.panel-footer        ; #footer
     [:span ; #todo-count
      [:strong num-active]
      (ts/pluralize-with num-active " item") " left  (" display-mode ")"]
     [:span "----"]
     [:div.btn-group.btn-group-xs
      [:button.btn.btn-xs {:type     :button :id :all :class "filters"
                :on-click #(flame/fire-event [:set-display-mode :all])} "All"]
      [:button.btn.btn-xs {:type     :button :id :active
                :on-click #(flame/fire-event [:set-display-mode :active])} "Active"]
      [:button.btn.btn-xs {:type     :button :id :completed
                :on-click #(flame/fire-event [:set-display-mode :completed])} "Completed"] ]
     [:span "----"]
     [:div.btn-group.btn-group-xs
      (when (pos? num-completed)
        [:button.btn.btn-xs {:type :button ; :id      :completed
                     :on-click #(flame/fire-event [:clear-completed])} "Clear Completed"])]
     ]))

(defn task-entry []
  [:header          ; #header
   [:h1 "todos"]
   [input-field
    {:id          "new-todo"
     :placeholder "What needs to be done?"
     :on-save     #(when (t/not-empty? (str/trim %))
                     (flame/fire-event [:add-todo %]))}]])

(defn todo-root []
  [:div
   [:section#todoapp
    [task-entry]
    (when (t/not-empty? (flame/watching [:todos]))
      [task-list])
    [footer-controls]]
   [:footer#info
    [:p "Double-click to edit a todo"]]])

;---------------------------------------------------------------------------------------------------

(defn ajax-says []
  [:div
   [:span {:style {:color :darkgreen}} [:strong "AJAX says: "]]
   [:span {:style {:font-style :italic}} nbsp nbsp (flame/watching [:ajax-response])]])

(defn root []       ; was simple-component
  [:div {:class "container"}
   ;[rbs/panel
   ; [rbs/label "React-Bootstrap Label!"]
   ; [rbs/button {:bs-size  :xsmall
   ;              :on-click #(js/alert "Hello from React-Bootstrap!")} "Click me!"]]

   [:hr]
   [:div
    [:p "I am a component!"]
    [:p.someclass
     "I have " [:strong "bold"]
     [:span {:style {:color "red"}} " and red"] " text."]]
   [:hr]
   [ajax-says] ; #todo Reagent => (r/comp ajax-says <arg1> <arg2> ...)
   [:hr]
   [:div
    [todo-root]] ; #todo Reagent => (r/comp todo-root <arg1> <arg2> ...)
   [:hr] ])










