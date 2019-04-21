(ns todomvc.flames
  (:require
    [todomvc.enflame :as flame]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn initialize-all []
  (flame/define-flame
    {:id            :display-mode
     :parent-flames [:app-state]
     :tx-fn         (fn [app-state -query-]
                        (:display-mode app-state))})

  (flame/define-flame
    {:id            :sorted-todos
     :parent-flames [:app-state]
     :tx-fn         (fn [app-state -query-]
                        (:todos app-state))})

  (flame/define-flame
    {:id            :todos
     :parent-flames [:sorted-todos]
     :tx-fn         (fn [sorted-todos -query-]
                        (vals sorted-todos))})

  (flame/define-flame
    {:id            :visible-todos
     :parent-flames [:todos :display-mode]
     :tx-fn         (fn [[todos showing] -query-]
                        (let [filter-fn (condp = showing
                                          :active (complement :completed)
                                          :completed :completed
                                          :all identity)]
                          (filter filter-fn todos)))})

  (flame/define-flame
    {:id            :all-complete?
     :parent-flames [:todos]
     :tx-fn         (fn [todos -query-]
                        (every? :completed todos))})

  (flame/define-flame
    {:id            :completed-count
     :parent-flames [:todos]
     :tx-fn         (fn [todos -query-]
                        (count (filter :completed todos)))})

  (flame/define-flame
    {:id            :footer-counts
     :parent-flames [:todos :completed-count]
     :tx-fn         (fn [[todos completed] -query-]
                        [(- (count todos) completed) completed])})

  (flame/define-flame
    {:id            :ajax-response
     :parent-flames [:app-state]
     :tx-fn         (fn [app-state -query-]
                        (:ajax-response app-state))})
  )
