(ns leiningen.testrun
  (:use [leiningen.core.eval :only [eval-in-project]]
        [clojure.algo.monads :only [domonad maybe-m]]))

(defn- test-command-names [test-commands]
  (keys test-commands))


(defn- make-init-form []
  `(do
     (require 'com.makkalot.testrun.testrun)))


(defn make-autotest-form [test-mode test-options interval]
  `(com.makkalot.testrun.testrun/autotest ~test-mode [~@test-options] ~interval))


(defn testrun
  "Lets run those tests"
  [project & args]
  (when-not
      (domonad maybe-m
                      [test-commands (get-in project [:cljsbuild :test-commands] {})
                       test-mode (first args)
                       test-options (test-commands test-mode)]
                      (if test-options
                        (do
                          (println "HERE ")
                          (println "DEPS " (:dependencies project))
                          (eval-in-project (-> project
                                              (update-in [:dependencies]
                                                (fnil into [])
                                                [['testrun "0.1.0"]]))
                                           `(com.makkalot.testrun/autotest ~test-mode [~@test-options] 1000)
                                           `(require 'com.makkalot.testrun)))
                        (println "None existing test mode : [options]"
                                 (test-command-names test-commands))))
    (println "Missing parameters!")))
