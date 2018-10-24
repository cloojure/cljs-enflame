(ns todomvc.topics
  (:require
    [re-frame.core :as rf]
    [todomvc.enflame :as flame] ))

(flame/define-topic!
  :showing
  (fn [db _]
    (:showing db)))

; #todo macro to insert topic as fn-name;  :sorted-todos => (fn sorted-todos-fn ...)
; #todo (flame/define-topic! :sorted-todos ...) => (fn sorted-todos-fn ...)
(flame/define-topic!
  :sorted-todos
  (fn [db _]
    (:todos db)))

; -------------------------------------------------------------------------------------
; Layer 3
;
; rf/reg-sub allows you to supply:
;
;   1. a function which returns the input signals. It can return either a single signal or
;      a vector of signals, or a map where the values are the signals.
;
;   2. a function which does the computation. It takes input values and produces a new
;      derived value.
;
; In the two simple examples at the top, we only supplied the 2nd of these functions.
; But now we are dealing with intermediate (layer 3) nodes, we'll need to provide both fns.
;
(flame/define-topic!
  :todos   ; usage:  (rf/subscribe [:todos])
  ; This function returns the input signals. In this case, it returns a single signal.
  ; Although not required in this example, it is called with two parameters
  ; being the two values supplied in the originating `(rf/subscribe X Y)`.
  ; X will be the query vector and Y is an advanced feature and out of scope
  ; for this explanation.
  (fn [query-v _]   ; signal function
    (rf/subscribe [:sorted-todos]))    ; returns a single input signal

  ; This 2nd fn does the computation. Data values in, derived data out.
  ; It is the same as the two simple subscription handlers up at the top.
  ; Except they took the value in app-db as their first argument and, instead,
  ; this function takes the value delivered by another input signal, supplied by the
  ; function above: (rf/subscribe [:sorted-todos])
  ;
  ; Subscription handlers can take 3 parameters:
  ;  - the input signals (a single item, a vector or a map)
  ;  - the query vector supplied to query-v  (the query vector argument to the "rf/subscribe")
  ;  - the 3rd one is for advanced cases, out of scope for this discussion.
  (fn [sorted-todos query-v _]   ; computation function
    (vals sorted-todos))) ; #todo sink ?

; So here we define the handler for another intermediate node.
; This time the computation involves two input signals. As a result note:
;   - the first function (which returns the signals) returns a 2-vector
;   - the second function (which is the computation) destructures this 2-vector as its first parameter
(flame/define-topic!
  :visible-todos

  ; Signal Function
  ; Tells us what inputs flow into this node.
  ; Returns a vector of two input signals (in this case)
  (fn [query-v _]
    [(rf/subscribe [:todos])
     (rf/subscribe [:showing])])

  ; Computation Function
  (fn [[todos showing] -query-]   ; that 1st parameter is a 2-vector of values
    (let [filter-fn (condp = showing
                      :active (complement :done)
                      :done   :done
                      :all    identity)]
      (filter filter-fn todos))))

; #todo predefine a built-in topic :rf/db. All user-defined topics are like:
;   (rs/define-topic!
;     [:rs/db :todos :showing]
;     (fn [[db todos showing] -query-] ; #todo add auto-destructure
;       <forms> ))

; Here is the example above rewritten using the sugar.
(flame/define-topic! :visible-todos-with-sugar
  :<- [:todos]
  :<- [:showing]
  (fn [[todos showing] -query-]
    (let [filter-fn (case showing
                      :active (complement :done)
                      :done   :done
                      :all    identity)]
      (filter filter-fn todos))))

(flame/define-topic! :all-complete?
  :<- [:todos]
  (fn [todos -query-]
    (every? :done todos)))

(flame/define-topic! :completed-count
  :<- [:todos]
  (fn [todos -query-]
    (count (filter :done todos))))

(flame/define-topic! :footer-counts
  :<- [:todos]
  :<- [:completed-count]
  (fn [[todos completed] -query-]
    [(- (count todos) completed) completed]))
















