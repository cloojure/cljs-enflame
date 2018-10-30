(ns tst.flintstones.bambam
  (:require
    #?@(:clj [[flintstones.test-clj   :refer [dotest is isnt is= isnt= testing define-fixture]]
              [flintstones.bambam :as bam]])
    #?@(:cljs [[flintstones.test-cljs :refer [dotest is isnt is= isnt= testing define-fixture]]
               [flintstones.bambam :as bam :include-macros true]])
  ))

(define-fixture :each
     {:enter (fn [] (println "*** TEST EACH *** - enter"))
      :leave (fn [] (println "*** TEST EACH *** - leave"))})
;--------------------------------------------------------------------------------------------------

(dotest
  (is= 2 (+ 1 1)))

(dotest
  (is= 5 (bam/add2 2 3)) ; this works
  (is= 3 (bam/logr-bambam
           (inc 0)
           (inc 1)
           (inc 2))))
