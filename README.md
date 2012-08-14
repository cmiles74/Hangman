# Hangman

Plays a non-interactive game of Hangman against the computer. When
run, fifteen games will be played using random words from the included
dictionary. After these games have been played, some rudimentary
statistics about these games will be provided (i.e., the average
score).

The dictionary of words is contained in "dictionary/words", you may
provide your own dictionary if you so desire.

## Building the Project

This application is written in Clojure, you will need to have a Java
runtime and Leiningen 2 installed in order to build the
project. Instructions on installing Leiningen 2 can be found on the
project's web page.

    https://github.com/technomancy/leiningen

Once Leiningen 2 is installed, you can build the project with the
following command:

    $ lein uberjar

This will create a new standalone JAR file in the "target" directory.

## Running the Project

Once you have the project built, you may generate a new test run.

	$ java -jar target/hangman-1.0-standalone.jar
	Playing 15 random games of hangman...

	SOLUTION: tantrum
	-------; score=0; status=KEEP_GUESSING
	-------; score=1; status=KEEP_GUESSING
	-------; score=2; status=KEEP_GUESSING
	-------; score=3; status=KEEP_GUESSING
	-a-----; score=4; status=KEEP_GUESSING
	-a--r--; score=5; status=KEEP_GUESSING
	-a--r--; score=6; status=KEEP_GUESSING
	-a--r--; score=7; status=KEEP_GUESSING
	ta-tr--; score=8; status=KEEP_GUESSING
	tantr--; score=9; status=KEEP_GUESSING
	tantru-; score=10; status=KEEP_GUESSING
	tantrum; score=11; status=GAME_WON
	...

You may also run the test suite with Leiningen's "test" target. In
addition to testing the provided functions, it will run through
fifteen test games and report the score for each as well as the
average score.

    $ lein test

## Strategies

Currently only one strategy ("frequency") is provided. This strategy
computes the frequency with which letters appear in candidate solution
words. This strategy lives in the "cmiles74.hangman.frequencystrategy"
namespace. By default, this is the strategy chosen; you can choose
your own strategy by importing it into your namespace.


    (ns cmiles74.hangman.core
      (:require [cmiles74.hangman.frequencystrategy :only guess :as freq]))

With the strategy imported, you can now use it to make guesses. When a
game is played with the "play-game" function, the strategy is the
first parameter.

    (play-game freq/guess dictionary (game "factual" 25))

The above function will play one game using the frequence guessing
strategy, the supplied list of solution words in the "dictionary"
variable and will play a game with the solution "factual" and will
tolerate 25 incorrect answers.

## Dictionaries

Functions are provided for loading a dictionary of words from disk as
well as for matching words against a provided criteria. All of these
functions are in the "dictionary" namespace.
