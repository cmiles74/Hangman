(ns ^{:doc "Provides an application that plays a game of Hangman
against the computer (that is, this game is not interactive)."}
  cmiles74.hangman.core
  (:gen-class)
  (:use [clojure.tools.logging]
        [clojure.tools.cli])
  (:require [cmiles74.hangman.dictionary :as dict]
            [cmiles74.hangman.frequencystrategy :only guess :as freq]
            [clojure.string :as string])
  (:import [java.util Date Random]
           [org.apache.commons.logging LogFactory]
           [org.apache.commons.logging Log]))

;; logger instance
(def LOGGER (. LogFactory getLog "cmiles74.hangman.core"))

;; game status values
(def GAME-STATUS {:won "GAME_WON"
                  :lost "GAME_LOST"
                  :guessing "KEEP_GUESSING"})

(defn game
  "Returns a map representing a new game."
  [solution max-wrong-guesses]
  {:status (:guessing GAME-STATUS)
   :score 0
   :solution (vec solution)
   :max-wrong-guesses max-wrong-guesses
   :incorrect-guessed #{}
   :correct-guessed (vec (for [index (range (count solution))] nil))
   :incorrect-words-guessed #{}})


(defn process-turn
  "Performs another turn on the part of the computer using the
  provided strategy and dictionary. This function returns a new game
  map that represents the state of the game and the conclusion of the
  computer's turn.

    strategy  A function that performs a guess for a word or letter
    dictionary  A sequence of possible answer words
    game  A map of the current game state"
  [strategy dictionary game]
  (let [;; calculate the computer's next guess
        guess (strategy dictionary game)

        ;; decide if this guess is good or bad
        good (cond
               (= :letter (first guess))
               (if ((into #{} (:solution game)) (nth guess 1))
                 true)

               (= :word (first guess))
               (if (= (apply str (:solution game)) (second guess))
                 true))]

    ;; return a new game state
    (merge game
           {:score (inc (:score game))
            :start-time (if (:start-time game) (:start-time game)
                            (Date.))}
           (if (not good)

             ;; incorrect guess
             (cond
               (= :letter (first guess))
               {:incorrect-guessed (conj (:incorrect-guessed game) (nth guess 1))}

               (= :word (first guess))
               {:incorrect-words-guessed (conj (:incorrect-words-guessed game)
                                               (last guess))})

             ;; correct guess
             (cond
               (= :letter (first guess))
               {:correct-guessed (vec (map #(if (= (nth (:solution game) %)
                                                   (nth guess 1))
                                              (nth guess 1)
                                              (nth (:correct-guessed game) %))
                                           (range (count (:solution game)))))}

               (= :word (first guess))
               {:correct-guessed (vec (last guess))})))))

(defn update-status
  "Returns a new game map that contains the current :status for the
  provided game. That is, if the game has been won, lost or is still
  in progress.

    game  A map of the game state"
  [game]
  (cond

    ;; won game
    (= (:solution game) (:correct-guessed game))
    (assoc game :status (:won GAME-STATUS))

    ;; lost game
    (<= (:max-wrong-guesses game) (:score game))
    (merge game {:status (:lost GAME-STATUS)
                 :score 25})

    ;; game in progress
    :else
    (assoc game :status (:guessing GAME-STATUS))))

(defn print-game
  "Returns a String representation of the provided game map.

    game  A map of game state"
  [game]
  (str (apply str (for [letter (:correct-guessed game)]
                    (if (nil? letter) "-" letter))) "; "
                    "score=" (:score game) "; "
                    "status=" (:status game)))

(defn play-game
  "Plays a game of Hangman with the computer until completion. The
  computer will use the provided strategy function to make its
  guesses.

    strategy  A function that guesses by word or letter
    dictionary  A sequence of possible solution words
    game  A map of the current game state

  The behavior of this function may be customized with the following
  keys.

    :output  Display progress on screen or to the log; :log, :console, nil"
  [strategy dictionary game & {:keys [output] :or {output :log}}]

  ;; play each game to completion
  (loop [game-this game]

    ;; update the status of the current game state
    (let [game-now (update-status game-this)]

      ;; log the progress of the current game
      (cond (= :log output)
            (info (print-game game-now))

            (= :console output)
            (println (print-game game-now)))

      ;; game in progress, recur with the next game state
      (if (= (:guessing GAME-STATUS) (:status game-now))
        (recur (process-turn strategy dictionary game-now))
        (let [end-time (Date.)
              run-time (- (.getTime end-time) (.getTime (:start-time game-now)))]

          ;; log the our run time
          (cond (= :log output)
                (info (str "Run time: " run-time "ms"))

                (= :console output)
                (println (str "Run time: " run-time "ms")))

          (merge game-now
                 {:end-time end-time
                  :run-time run-time}))))))

(defn main
  [& args]
  (let [[options args banner]
        (cli args
             ["-h" "--help" "Show usage information" :default false :flag true]
             ["-d" "--dictionary" "Path to an alternative dictionary file"]
             ["-n" "--number" "Number of random games to play" :default "15"]
             ["-s" "--solutions" "A comma separated list of solutions"]
             ["-g" "--guesses" "Maximum number of guesses allowed" :default "11"])]
    (cond
      (:help options)
      (println banner)

      :else
      (let [random (Random. (.getTime (Date.)))

            dict-path (if (:dictionary options) (:dictionary options)
                          "dictionary/words.txt")

            dictionary (dict/load-dictionary dict-path)

            num-games (Integer/parseInt (:number options))

            solution-words (if (:solutions options)
                             (string/split (:solutions options) #",")
                             dictionary)

            solutions (for [index (range num-games)]
                        [index (nth solution-words
                                    (.nextInt random (count solution-words)))])

            guesses (Integer/parseInt (:guesses options))

            ;; a sequence of our lazily computed games
            games (for [solution solutions]
                    (do (info "")
                        (info (str "GAME #" (inc (first solution))))
                        (info "SOLUTION:" (second solution))
                        (play-game freq/guess dictionary
                                   (game (second solution) guesses)
                                   :output :log)))

            ;; compute some stats on the games
            average-score (float (/ (apply + (pmap :score games)) (count solutions)))
            lost (reduce + (pmap #(if (= "GAME_LOST" (:status %)) 1 0) games))
            won (- (count solutions) lost)
            total-run (apply + (map :run-time games))
            average-run (float (/ (apply + (map :run-time games))
                                  (count solutions)))
            shortest-run (first (sort (map :run-time games)))]

        ;; display some stats on the games
        (info "RESULTS")
        (info "    Average score:" average-score)
        (info "              Won:" won)
        (info "             Lost:" lost)
        (info (str "Average Time/Game: " average-run "ms"))
        (info (str "    Shortest Game: " shortest-run "ms")))))

  ;; shutdown the agent thread pool
  (shutdown-agents))

(defn -main
  "Bootstraps the application"
  [& args]
  (apply main args))
