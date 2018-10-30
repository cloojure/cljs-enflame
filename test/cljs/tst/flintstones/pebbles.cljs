(ns tst.flintstones.pebbles
  (:require
    [clojure.string :as str]
    [flintstones.test-cljs :refer [dotest is isnt is= isnt= testing use-fixtures]] ))

(use-fixtures :each
  {:enter (fn [] (println "*** TEST EACH *** - enter"))
   :leave (fn [] (println "*** TEST EACH *** - leave"))})

(dotest
  (is= 2 (+ 1 1))   ; this works
  (let [data {:a 1 :b 2}
        {:keys [a b]} data]
    (is= [a b] [1 2])
    (isnt false))

  ; This must be commented-out, or it will throw an Error since `pebbles.js` is only
  ; included via the <script> tag in `index.html`. The Doo testing framework doesn't
  ; use `index.html`, so we cannot access items from `pebbles.js` in these tests.
  (comment
    (println "js/pebblesInfo:  " js/pebblesInfo)))

