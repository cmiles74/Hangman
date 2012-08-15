(ns ^{:doc "Provides functions for managing and querying a dictionary
of words."}
  cmiles74.hangman.dictionary
  (:require [clojure.string :as string]))

(defn load-dictionary
  "Loads in a list of words from the line delimited dictionary file at
  the provided file and returns a sequence of those words.

    path  A String representation of a file system path"
  [path]
  (string/split (slurp path) #"\s+"))

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
    blacklist-letters A set of letters that shouldn't be present in
      matching words
    criteria  A sequence representing the current criteria, i.e.
      \"[nil nil l l nil]\""
  [dictionary blacklist-letters criteria]
  (filter #(not (nil? %))
          (for [word dictionary :when (word-matches criteria word)]
            (if (every? #(not (blacklist-letters %)) (set word))
              word)))
  ;(filter #(word-matches criteria %) dictionary)
)

(def query-words (memoize query-words-raw))

(defn candidate-words
  "Returns a sequence of words from the provided dictionary that both
  satisfy the criteria and do not appear in the supplied blacklist.

    dictionary  A sequence of solution words
    blacklist-words  A set of words to be excluded
    blacklist-letters A set of letters that shouldn't be present in
      matching words
    criteria   A sequence representing the current criteria, i.e.
      \"[nil nil l l nil]\""
  [dictionary blacklist-letters blacklist-words criteria]
  (filter #(not (blacklist-words %))
          (query-words dictionary blacklist-letters criteria)))