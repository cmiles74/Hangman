(ns ^{:doc "Provides a strategy for playing Hangman based on the
  frequency with which letters appear in possible solution words. To
  utilize this strategy, import the \"guess\" function."}
  cmiles74.hangman.frequencystrategy
  (:import [java.util Date Random]))

;; random number generator
(def random (Random. (.getTime (Date.))))

(defn- letter-frequency-raw
  "Computes the frequency each letter appears in the dictionary words
  for the specified position.

    dictionary  A sequence of solution words
    position  The letter position of the solution for which frequencies
      will be calculated"
  [dictionary position]

  ;; retrieve all of the letters at the given position
  (let [letters-in (sort

                    ;; exclude any nil values
                    (filter #(not (nil? %))

                            ;; retrieve each letter in the supplied
                            ;; position of each word
                            (for [word dictionary]

                              ;; don't use words that aren't long enough
                              (if (< position (count word))
                                (nth word position)))))]

    ;; compute the frequency for each letter
    (loop [letter-this (first letters-in)
           letters letters-in
           frequency {}]

      ;; recur with our results if we have more letters
      (if (not (nil? letter-this))
        (recur (first (rest letters))
               (rest letters)

               ;; update the frequence map for this letter
               (if (not (nil? (frequency letter-this)))
                 (assoc frequency letter-this (inc (frequency letter-this)))
                 (assoc frequency letter-this 1)))

        ;; sort our frequencies by value (most popular letter first)
        (sort #(compare (last %2) (last %1))
              frequency)))))

(def letter-frequency (memoize letter-frequency-raw))

(defn word-matches
  "Returns true if the provided word matches the specified criteria.

    criteria  A sequence representing the current criteria, i.e.
      \"[nil nil l l nil]\"
    word  The word that will be validated against the criteria"
  [criteria word]

  ;; they need to be the same length
  (and (= (count word) (count criteria))
       (every? #(= true %)

               ;; match each word letter against its matching criteria
               ;; position
               (map #(or (nil? (nth criteria %))
                         (= (nth criteria %) (nth word %)))
                    (range (count criteria))))))

(defn query-words-raw
  "Returns a sequence of words in the provided dictionary that match
  the specified criteria.

    dictionary  A sequence of solution words
    criteria  A sequence representing the current criteria, i.e.
      \"[nil nil l l nil]\""
  [dictionary criteria]
  (filter #(word-matches criteria %) dictionary))

(def query-words (memoize query-words-raw))

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
        candidates (map #(filter (fn [frequency]
                                     (not (or ((:incorrect-guessed game)
                                               (first frequency))
                                              ((into #{} (:correct-guessed game))
                                               (first frequency)))))
                                 (nth frequencies %))
                        (range (count (:solution game))))

        ;; collect the indexes with correct guesses
        good-indices (map #(if (not (nil? (nth (:correct-guessed game) %)))%)
                          (range (count (:solution game))))

        ;; compile a list of best guesses for each letter position
        best-guesses (sort #(compare (last (second %2))
                                     (last (second %1)))

                           ;; remove the frequencies for positions
                           ;; we've guessed correctly
                           (reduce dissoc
                                   (apply merge
                                          (map
                                           #(hash-map % (first (nth candidates %)))
                                           (range (count candidates))))
                                   good-indices))

        ;; calculate which of our best guesses occur for more than one
        ;; letter position
        combined (loop [matches {}
                        guess-this (first best-guesses)
                        guesses (rest best-guesses)]

                   ;; loop until we've checked all of our guesses
                   (if (not (nil? guess-this))

                     ;; update our map of letters to [[positions], score]
                     (recur (merge matches
                                   {(first (nth guess-this 1))
                                    [(conj (if (matches (first (nth guess-this 1)))
                                             (first (matches
                                                     (first (nth guess-this 1))))
                                             [])
                                           (first guess-this))

                                     ;; combine the scores
                                     (+ (if (last (matches
                                                   (first (nth guess-this 1))))
                                          (last (matches (first (nth guess-this 1))))
                                          0)
                                        (last (nth guess-this 1)))]})
                            (first guesses) (rest guesses))

                     ;; return our map of combined guesses
                     matches))

        ;; sort our combined guesses by score
        combined-sorted (sort #(compare (last (second %2)) (last (second %1)))
                              combined)]

    ;; return our best guess
    [:letter
     (first (first combined-sorted))
     (first (second (first combined-sorted)))]))

(defn guess-word
  "Returns a guess for the whole word. The guess will be a sequence
  where the first item is a the keyword :word and the second item is
  the word being guessed. For example: {:word \"miles\"}

    dictionary  A sequence of solution words
    game  A map of the current game state"
  [dictionary game] [:word (nth
  dictionary (.nextInt random (count dictionary)))])

(defn guess
  "Returns a guess, the represents the computers turn in a game of
  Hangman. This will be a sequence where the first item is a keyword
  representing the type of guess (:letter or :word) and the second
  item is the letter or word being guessed. For example:

    {:letter \"m\"} or {:word \"miles\"}

    dictionary  A sequence of solution words
    game  A map of the current game state"
  [dictionary game]
  (let [candidate-words (query-words dictionary (:correct-guessed game))
        letter-guess (guess-letter candidate-words game)]

    (cond
      (> 2 (count candidate-words))
      (guess-word candidate-words game)

      :else
      (guess-letter candidate-words game))))
