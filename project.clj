(defproject cmiles74/hangman "1.0"
  :description "A program that plays 'Hangman'"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [commons-logging "1.1.1"]
                 [org.clojure/tools.cli "0.2.1"]
                 [log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :main cmiles74.hangman.core)
