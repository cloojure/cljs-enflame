(ns todomvc.topics
  (:require
    [todomvc.enflame :as flame] ))

; #todo these should all return a map

(defn register-topics! []
  (flame/define-topic! :showing
    [:db]
    (fn [db -query-]
      (:showing db)))

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


  (flame/define-topic! :all-complete?
    [:todos]
    (fn [todos -query-]
      (every? :done todos)))

  (flame/define-topic! :completed-count
    [:todos]
    (fn [todos -query-]
      (count (filter :done todos))))

  (flame/define-topic! :footer-counts
    [:todos :completed-count]
    (fn [[todos completed] -query-]
      [(- (count todos) completed) completed])))
