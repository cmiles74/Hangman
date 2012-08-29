(ns ^{:doc "Provides a strategy for playing Hangman based on the
  frequency with which letters appear in possible solution words. To
  utilize this strategy, import the \"guess\" function."}
  cmiles74.hangman.frequencystrategy
  (:require [cmiles74.hangman.dictionary :as dict])
  (:import [java.util Date Random]))

;; random number generator
(def random (Random. (.getTime (Date.))))

(defn letter-frequency-raw
  "Computes the frequency each letter appears in the dictionary words
  for the specified position.

    dictionary  A sequence of solution words
    position  The letter position of the solution for which frequencies
      will be calculated"
  [dictionary position]

  ;; retrieve all of the letters at the given position
  (let [letters-in (sort

                    ;; retrieve each letter in the supplied position
                    ;; of each word
                    (for [word dictionary :when (not (nil? word))]

                      ;; don't use words that aren't long enough
                      (if (< position (count word))
                        (nth word position))))]

    ;; compute the frequency for each letter and sort our highest
    ;; scores to the top
    (sort #(compare (last %2) (last %1))

          ;; reduce our frequencies to map where the letter is the key
          ;; and the score is the value
          (reduce (fn [frequency letter]
                    (if (not (nil? (frequency letter)))
                      (assoc frequency letter (inc (frequency letter)))
                      (assoc frequency letter 1)))
                  {} letters-in))))

(def letter-frequency (memoize letter-frequency-raw))

(defn candidate-frequencies
  "Returns a sequences of letter frequency data selected from the
   provided sequences of frequency data with the blacklisted letters
   and the correctly guessed criteria letters removed.

    frequencies A sequence of letter frequency data blacklist A set of
    letters already guessed incorrectly criteria A sequence
    representing the current criteria, i.e.  \"[nil nil l l nil]\""
  [frequencies blacklist criteria]
  (let [criteria-set (set criteria)]
    (map (fn [freq]
           (filter #(not (or (blacklist (first %1))
                             (criteria-set (first %1))))
                   freq))
         frequencies)) )

(defn guess-letter
  "Returns a guess for a letter. The guess will be a sequence where
  the first item is they keyword :letterm the second item is the
  letter being guessed and the last item is the position in the word
  that is most likely to have the letter. For example {:letter \"m\" 1}

    dictionary  A sequence of solution words
    game  A map of the current game state"
  [dictionary game]

  (let [;; calculate the letter frequencies for each position
        frequencies (map #(letter-frequency dictionary %)
                         (range (count (:solution game))))

        ;; eliminate our guesses (good and bad)
        candidates (candidate-frequencies frequencies
                                          (:incorrect-guessed game)
                                          (:correct-guessed game))

        ;; compile a list of best guesses for each letter position
        best-guesses (sort

                      ;; sort by our highest scoring positions
                      #(compare (last (second %2)) (last (second %1)))

                      ;; map our frequencies to letter positions
                      (apply merge
                             (map #(if (first %1) (hash-map %2 (first %1)))
                                  candidates
                                  (range (count candidates)))))]

    ;; only proceed if we have some possible guesses
    (if (seq best-guesses)

      (let [;; calculate which of our best guesses occur for more than
            ;; one letter position and sort the best scores to the top
            combined (sort #(compare (last (second %2)) (last (second %1)))

                           ;; reduce our guesses to one per letter
                           ;; with a list of positions and one
                           ;; combined score
                           (reduce
                            (fn [matches guess]
                              (merge matches
                                     {;; the letter
                                      (first (nth guess 1))

                                      ;; positions where this letter appears
                                      [(conj (if (matches (first (nth guess 1)))
                                               (first (matches
                                                       (first (nth guess 1))))
                                               [])
                                             (first guess))

                                       ;; the score for this letter
                                       ;; and it's largest position
                                       ;; score
                                       (if (last (matches (first (nth guess 1))))
                                         (if (< (last (matches
                                                       (first (nth guess 1))))
                                                (last (nth guess 1)))
                                           (last (nth guess 1))
                                           (last (matches (first (nth guess 1)))))
                                         (last (nth guess 1)))]}))
                            {} best-guesses))]

        (if (= #{nil} (set (:correct-guessed game)))

          ;; prefer a vowel
          (let [vowels (filter #(#{\a \e \i \o \u} (first %))
                               combined)]
            (if (seq? vowels)
              [:letter (first (first vowels))
               (first (second (first vowels)))]

              ;; fall back to our best guess
              [:letter (first (first combined))
               (first (second (first combined)))]))

          ;; return our best guess
          [:letter (first (first combined))
           (first (second (first combined)))])))))

(defn guess-word
  "Returns a guess for the whole word. The guess will be a sequence
  where the first item is a the keyword :word and the second item is
  the word being guessed. For example: {:word \"miles\"}

    dictionary  A sequence of solution words"
  [dictionary]
  (if (< 0 (count dictionary))
    [:word (nth dictionary (.nextInt random (count dictionary)))]))

(defn guess
  "Returns a guess, the represents the computers turn in a game of
  Hangman. This will be a sequence where the first item is a keyword
  representing the type of guess (:letter or :word) and the second
  item is the letter or word being guessed. For example:

    {:letter \"m\"} or {:word \"miles\"}

    dictionary  A sequence of solution words
    game  A map of the current game state"
  [dictionary game]
  (let [candidate-words (dict/candidate-words dictionary
                                              (:incorrect-guessed game)
                                              (:incorrect-words-guessed game)
                                              (:correct-guessed game))

        letter-guess (guess-letter candidate-words game)]

    (cond
      (> 2 (count candidate-words))
      (guess-word candidate-words)

      (= (dec (:max-wrong-guesses game)) (:score game))
      (guess-word

       ;; assume our letter guess is correct and get the next round of
       ;; candidate words
       (dict/query-words candidate-words
                         (:incorrect-guessed game)
                         (reduce #(assoc %1 %2 (second letter-guess))
                                 (:correct-guessed game)
                                 (last letter-guess))))

      :else
      letter-guess)))
