(ns todomvc.flames
  (:require
    [todomvc.enflame :as flame]))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

(defn initialize []
  (flame/define-flame
    {:id              :display-mode
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-]
                        (:display-mode app-state))})

  (flame/define-flame
    {:id              :sorted-todos
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-]
                        (:todos app-state))})

  (flame/define-flame
    {:id              :todos
     :reactive-inputs [:sorted-todos]
     :tx-fn           (fn [sorted-todos -query-]
                        (vals sorted-todos))})

  (flame/define-flame
    {:id              :visible-todos
     :reactive-inputs [:todos :display-mode]
     :tx-fn           (fn [[todos showing] -query-]
                        (let [filter-fn (condp = showing
                                          :active (complement :completed)
                                          :completed :completed
                                          :all identity)]
                          (filter filter-fn todos)))})

  (flame/define-flame
    {:id              :all-complete?
     :reactive-inputs [:todos]
     :tx-fn           (fn [todos -query-]
                        (every? :completed todos))})

  (flame/define-flame
    {:id              :completed-count
     :reactive-inputs [:todos]
     :tx-fn           (fn [todos -query-]
                        (count (filter :completed todos)))})

  (flame/define-flame
    {:id              :footer-counts
     :reactive-inputs [:todos :completed-count]
     :tx-fn           (fn [[todos completed] -query-]
                        [(- (count todos) completed) completed])})

  (flame/define-flame
    {:id              :ajax-response
     :reactive-inputs [:app-state]
     :tx-fn           (fn [app-state -query-]
                        (:ajax-response app-state))})
  )
