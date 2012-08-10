(ns cmiles74.hangman.frequencystrategy
  (:import [java.util Date Random]))

;; random number generator
(def random (Random. (.getTime (Date.))))

(defn letter-frequency
  "Computes the frequency each letter appears in the dictionary words
  for the specified position."
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

        ;; sort our frequencies by value
        (sort #(compare (last %2) (last %1))
              frequency)))))

(defn word-matches
  "Returns true if the provided word matches the specified criteria."
  [criteria word]
  (and (= (count word) (count criteria))
       (every? #(= true %)
               (map #(or (nil? (nth criteria %))
                         (= (nth criteria %) (nth word %)))
                    (range (count criteria))))))

(defn query-words
  "Returns a sequence of words in the provided dictionary that match
  the specified criteria."
  [dictionary criteria]
  (filter #(word-matches criteria %) dictionary))

(defn guess-letter
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

        ;; compile a list of best guesses for each position
        best-guesses (sort #(compare (last (second %2))
                                     (last (second %1)))

                           ;; remove the frequencies for positions
                           ;; we've guessed correctly
                           (reduce dissoc
                                   (apply merge
                                          (map
                                           #(hash-map % (first (nth candidates %)))
                                           (range (count candidates))))
                                   good-indices))]

    ;; return our one best guess
    [:letter (nth (flatten (first best-guesses)) 1)]))

(defn guess-word
  [dictionary game]
  [:word (nth dictionary (.nextInt random (count dictionary)))])

(defn guess
  [dictionary game]
  (let [candidate-words (query-words dictionary (:correct-guessed game))]
    (cond
      (> 2 (count candidate-words))
      (guess-word candidate-words game)

      :else
      (guess-letter candidate-words game))))
