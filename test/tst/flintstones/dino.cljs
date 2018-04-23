(ns tst.flintstones.dino
  (:require
    [cljs.test :refer-macros [deftest is async use-fixtures]]
    [dinoPhony]))

(use-fixtures :once
  {:before (fn [] (println "Beginning dino tests..."))
   :after  (fn [] (println "Finished dino tests..."))})

(deftest t-will-succeed (is (= 5 (+ 2 3))))
; (deftest t-will-fail (is (= 95 (+ 2 3))))

(deftest t-dino
  (println "globalObject:  " js/globalObject)
  (let [result-7 (-> js/globalObject .-b (+ 5))
        dino     (js/makeDino)
        desc     (.-desc dino)
        said     (.says dino 5)]
    (println "(-> % .-b (+ 5) =>" result-7)
    (println "(js/makeDino) =>" dino)
    (println "dino.desc => " desc)
    (println "dino.says(5) => " said)
    (is (= 7 result-7))
    (is (= desc "blue dino-dog"))
    (is (= said "Ruff-Ruff-Ruff-Ruff-Ruff!"))))
