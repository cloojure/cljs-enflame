(ns todomvc.facets
  (:require
    [todomvc.enflame :as flame] ))

; NOTE:  it seems this must be in a *.cljs file or it doesn't work on figwheel reloading
(enable-console-print!)

; #todo these should all return a map

(defn register-facets! []
  (flame/define-facet
    {:facet-id     :display-mode
     :input-facets [:app-state]
     :tx-fn        (fn [app-state -query-]
                     (:display-mode app-state))})

  (flame/define-facet
    {:facet-id     :sorted-todos
     :input-facets [:app-state]
     :tx-fn        (fn [app-state -query-]
                     (:todos app-state))})

  (flame/define-facet
    {:facet-id     :todos
     :input-facets [:sorted-todos]
     :tx-fn        (fn [sorted-todos -query-]
                     (vals sorted-todos))})

  (flame/define-facet
    {:facet-id     :visible-todos
     :input-facets [:todos :display-mode]
     :tx-fn        (fn [[todos showing] -query-]
                     (let [filter-fn (condp = showing
                                       :active (complement :completed)
                                       :completed :completed
                                       :all identity)]
                       (filter filter-fn todos)))})

  (flame/define-facet
    {:facet-id     :all-complete?
     :input-facets [:todos]
     :tx-fn        (fn [todos -query-]
                     (every? :completed todos))})

  (flame/define-facet
    {:facet-id     :completed-count
     :input-facets [:todos]
     :tx-fn        (fn [todos -query-]
                     (count (filter :completed todos)))})

  (flame/define-facet
    {:facet-id     :footer-counts
     :input-facets [:todos :completed-count]
     :tx-fn        (fn [[todos completed] -query-]
                     [(- (count todos) completed) completed])})

  (flame/define-facet
    {:facet-id     :ajax-response
     :input-facets [:app-state]
     :tx-fn        (fn [app-state -query-]
                     (:ajax-response app-state))})
)
