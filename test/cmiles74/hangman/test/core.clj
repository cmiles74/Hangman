(ns ^{:doc "Provides a test suite for the Hangman playing application
and it's strategies."}
  cmiles74.hangman.test.core
  (:use [clojure.test])
  (:require [cmiles74.hangman.core :as hangman]
            [cmiles74.hangman.dictionary :as dict]
            [cmiles74.hangman.frequencystrategy :only guess :as freq]))

(def DICTIONARY (dict/load-dictionary "dictionary/words.txt"))

(deftest dictionary

  (testing "word-matches"
    (is (dict/word-matches [nil nil \i \l \y] "emily"))

    (testing "word-matches, fail"
      (is (not (dict/word-matches [nil nil \i \l \y] "miles"))))

    (testing "query-words"
      (let [matches (dict/query-words ["emily" "doily" "joanna" "miles"]
                                      #{}
                                      [nil nil nil \l \y])]
        (is (and (= 2 (count matches))
                 (= #{"emily" "doily"} (set matches))))))

    (testing "query-words, fail"
      (let [matches (dict/query-words ["emily" "rianna" "joanna" "miles"]
                                      #{}
                                      [nil nil nil \l \y])]
        (is (not (and (= 2 (count matches))
                      (= #{"emily" "doily"} (set matches)))))))))

(deftest strategy

  (testing "letter-frequency"
    (let [dictionary ["miles" "emily" "joanna" "derrida" "emil"]
          frequencies (freq/letter-frequency dictionary 3)]
      (is (filter #(and (= \l (first %)) (= 2 (second %)))
                  frequencies))))

  (testing "guess-letter"
    (let [dictionary ["miles" "emily" "joanna" "derrida"]
          criteria [nil nil \i \l \y]
          candidates (dict/candidate-words dictionary #{} #{} criteria)
          game (assoc (hangman/game "emily" 25)
                 :correct-guessed criteria)
          guess (freq/guess-letter candidates game)]
      (is (or (= \e (second guess))
              (= \m (second guess))))))

  (testing "guess-word"
    (let [dictionary ["miles" "emily" "joanna" "derrida"]
          guess (freq/guess-word dictionary)]
      (is (and ((set dictionary) (second guess))))))

  (testing "guess, letter"
    (let [dictionary ["miles" "emily" "joanna" "derrida" "doily"]
          criteria [nil nil nil \l nil]
          candidates (dict/candidate-words dictionary #{} #{} criteria)
          game (assoc (hangman/game "emily" 25)
                 :correct-guessed criteria)
          guess (freq/guess candidates game)]
      (is (or (= \i (second guess))
              (= \y (second guess))))))

  (testing "guess, word"
    (let [dictionary ["miles" "emily" "joanna" "derrida"]
          criteria [nil nil \i \l \y]
          candidates (dict/candidate-words dictionary #{} #{} criteria)
          game (assoc (hangman/game "emily" 25)
                 :correct-guessed criteria)
          guess (freq/guess candidates game)]
      (is (and (= :word (first guess))
               (= "emily" (second guess)))))))

(deftest core

  (testing "Sample Run"
    (let [solutions ["comaker" "cumulative" "eruptive" "factual" "monadism" "mus"
                     "nagging" "oses" "remembered" "spodumenes" "stereoisomers"
                     "toxics" "trichromats" "triose" "uninformed"]
          games (for [solution solutions]
                  (hangman/play-game freq/guess
                                     DICTIONARY
                                     (hangman/game solution 11)
                                     :output nil))
          average-score (float (/ (apply + (map :score games)) 15))]

      (dorun
       (for [game games]
         (println (str (apply str (:solution game)) " = " (:score game)))))

      (println "Average score:" average-score)

      (is (and (> 2 (count (filter #(= "GAME_LOST" (:status %)) games)))
               (> 8.667 average-score))))))