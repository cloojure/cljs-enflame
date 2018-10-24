(ns todomvc.topics
  (:require
    [re-frame.core :as rf]
    [todomvc.enflame :as flame] ))

(defn register-topics! []

  (flame/define-topic! :showing
    [:db]
    (fn [db -query-]
      (:showing db)))

  ; #todo macro to insert topic as fn-name;  :sorted-todos => (fn sorted-todos-fn ...)
  ; #todo (flame/define-topic! :sorted-todos ...) => (fn sorted-todos-fn ...)
  (flame/define-topic! :sorted-todos
    [:db]
    (fn [db -query-]
      (:todos db)))

  (flame/define-topic! :todos
    [:sorted-todos]
    (fn [sorted-todos -query-]
      (vals sorted-todos)))

  (flame/define-topic! :visible-todos
    [:todos :showing]
    (fn [[todos showing] -query-]
      (let [filter-fn (condp = showing
                        :active (complement :done)
                        :done :done
                        :all identity)]
        (filter filter-fn todos))))

  ; #todo predefine a built-in topic :rf/db. All user-defined topics are like:
  ;   (rs/define-topic!
  ;     [:rs/db :todos :showing]
  ;     (fn [[db todos showing] -query-] ; #todo add auto-destructure
  ;       <forms> ))

  ; Here is the example above rewritten using the sugar.
  (flame/define-topic-old! :visible-todos-with-sugar
    :<- [:todos]
    :<- [:showing]
    (fn [[todos showing] -query-]
      (let [filter-fn (case showing
                        :active (complement :done)
                        :done :done
                        :all identity)]
        (filter filter-fn todos))))

  (flame/define-topic-old! :all-complete?
    :<- [:todos]
    (fn [todos -query-]
      (every? :done todos)))

  (flame/define-topic-old! :completed-count
    :<- [:todos]
    (fn [todos -query-]
      (count (filter :done todos))))

  (flame/define-topic-old! :footer-counts
    :<- [:todos]
    :<- [:completed-count]
    (fn [[todos completed] -query-]
      [(- (count todos) completed) completed]))

  )
