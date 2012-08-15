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

Once you have the project built, you may generate a new test run. It
will play fifteen random games where a maximum of eleven incorrect
guesses is allowed.

	$ java -jar target/hangman-1.0-standalone.jar

    GAME #1
	SOLUTION: stepson
	-------; score=0; status=KEEP_GUESSING
	--e----; score=1; status=KEEP_GUESSING
	s-e-s--; score=2; status=KEEP_GUESSING
	ste-s--; score=3; status=KEEP_GUESSING
	ste-so-; score=4; status=KEEP_GUESSING
	ste-son; score=5; status=KEEP_GUESSING
	stepson; score=6; status=GAME_WON
	Run time: 987ms

    ...play more games...

	GAME #15
	SOLUTION: oxo
	---; score=0; status=KEEP_GUESSING
	---; score=1; status=KEEP_GUESSING
	o-o; score=2; status=KEEP_GUESSING
	oxo; score=3; status=GAME_WON
	Run time: 51ms
	RESULTS
		Average score: 8.266666
				  Won: 14
				 Lost: 1
	Average Time/Game: 490.13333ms
		Shortest Game: 51ms


You can pass several command line parameters in order to customize the
behavior of the application. You may specify your own dictionary of
words, the number of games to play and provide the solutions for the
games (comma separated, no spaces).

    $ java -jar target/hangman-1.0-standalone.jar -s house,cards

The incovation above will play two games, one for "house" and one for
"cards". The flags are...

* "-h" or "--help" will provide usage information
* "-d" or "--dictionary" let you set the path to the word dictionary
* "-n" or "--number" allows you to specify the number of games to play
* "-g" or "--guesses" sets the maximum wrong guesses allowed
* "-s" or "--solutions" lets you provide a comma separated list of
  solutions

If no solutions are provided, a random set of solutions will be
selected from the dictionary file.

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

Letter frequency data for a given dictionary is cached between runs,
this strategy should get faster the more games that it plays. For
instance, during one 15 game run on an averagely spiffy workstation
this strategy took 1244ms for the longest game and 104ms for the
quickest, averageing 419ms per game. For a run of 100 games, the
shortest game completed in 49ms and on average games ran for 311ms.

## Dictionaries

Functions are provided for loading a dictionary of words from disk as
well as for matching words against a provided criteria. All of these
functions are in the "dictionary" namespace.
