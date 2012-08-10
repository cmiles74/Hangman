(ns ^{:doc "Provides an application that plays a game of Hangman
against the computer (that is, this game is not interactive)."}
  cmiles74.hangman.core
  (:gen-class)
  (:use [clojure.tools.logging])
  (:require [clojure.string :as string]
            [cmiles74.hangman.frequencystrategy :only guess :as freq])
  (:import [java.util Date Random]
           [org.apache.commons.logging LogFactory]
           [org.apache.commons.logging Log]))

;; logger instance
(def LOGGER (. LogFactory getLog "cmiles74.hangman.core"))

(def ^:dynamic *DICTIONARY* (atom []))

;; game status values
(def GAME-STATUS {:won "GAME_WON"
                  :lost "GAME_LOST"
                  :guessing "KEEP_GUESSING"})

(defn load-dictionary
  "Loads in a list of words from the line delimited dictionary file at
  the provided file and returns a sequence of those words.

    path  A String representation of a file system path"
  [path]
  (string/split (slurp path) #"\s+"))

(defn game
  "Returns a map representing a new game."
  [solution max-wrong-guesses]
  {:status (:guessing GAME-STATUS)
   :score 0
   :solution (vec solution)
   :max-wrong-guesses max-wrong-guesses
   :incorrect-guessed #{}
   :correct-guessed (vec (for [index (range (count solution))] nil))
   :incorrect-words-guessed []})


(defn process-turn
  "Performs another turn on the part of the computer using the
  provided strategy and dictionary. This function returns a new game
  map that represents the state of the game and the conclusion of the
  computer's turn.

    strategy  A function that performs a guess for a word or letter
    dictionary  A sequence of possible answer words
    game  A map of the current game state"
  [strategy dictionary game]
  (let [;; get the computer's next guess
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
           {:score (inc (:score game))}
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

    game  A map of the current game state"
  [game]
  (cond

    ;; won game
    (= (:solution game) (:correct-guessed game))
    (assoc game :status (:won GAME-STATUS))

    ;; lost game
    (<= (:max-wrong-guesses game)
        (+ (count (:incorrect-guessed game))
           (count (:incorrect-words-guessed game))))
    (assoc game :status :lost GAME-STATUS)

    ;; game in progress
    :else
    (assoc game :status (:guessing GAME-STATUS))))

(defn print-game
  "Returns a String representation of the provided game map."
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

    :output  Display progress on screen or to the log; :log, :console"
  [strategy dictionary game & {:keys [output] :or {output :log}}]

  ;; play each game to completion
  (loop [game-this game]

    ;; update the status of the current game state
    (let [game-now (update-status game-this)]

      ;; log the progress of the current game
      (if (= :log output)
        (info (print-game game-now))
        (println (print-game game-now)))

      ;; game in progress, recur with the next game state
      (if (= (:guessing GAME-STATUS) (:status game-now))
        (recur (process-turn strategy dictionary game-now))
        game-now))))

(defn main
  [& args]

  ;; load in the dictionary
  (reset! *DICTIONARY* (load-dictionary "dictionary/words.txt"))

  ;; play fifteen random games
  (info "Playing 15 random games of hangman...")
  (let [random (Random. (.getTime (Date.)))

        ;; our randomly chosen solutions
        solutions (for [index (range 15)]
                    (nth @*DICTIONARY* (.nextInt random (count @*DICTIONARY*))))

        ;; a sequence of our lazily computed games
        games (for [solution solutions]
                (do (info "")
                    (info "SOLUTION:" solution)
                    (play-game freq/guess @*DICTIONARY* (game solution 25))))]

    ;; display some stats on our results
    (let [average-score (float (/ (apply + (map :score games)) 15))]
      (info "")
      (info "RESULTS")
      (info "  Average score:" average-score))))

(defn -main
  "Bootstraps the application"
  [& args]
  (main args))
