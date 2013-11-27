(ns leiningen.fschange
  (:refer-clojure :exclude [do])
  (:require [leiningen.core.main :as main]
            [clojure.java.io :as io]
            [clojure.java.shell  :refer [sh]]
            [com.makkalot.testrun :refer [schedule scan-and-react!
                                          current-tracker scan scheduled-futures]])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]
           [java.io File]))

(defn- conj-to-last [coll x]
  (update-in coll [(dec (count coll))] conj x))

(defn- butlast-char
  "Removes the last character in the string."
  [s]
  (subs s 0 (dec (count s))))

(defn- pop-if-last
  "Pops the collection if (pred (peek coll)) is truthy."
  [coll pred]
  (if (pred (peek coll))
    (pop coll)
    coll))

(defn ^:internal group-args
  ([args]  (-> (reduce group-args [[]] args)
               (pop-if-last empty?)))
  ([groups arg]
     (cond (coll? arg) (-> (pop-if-last groups empty?)
                           (conj arg []))
           (.endsWith arg ",") (-> groups
                                   (conj-to-last (butlast-char arg))
                                   (conj []))
           :else (conj-to-last groups arg))))

(defn- react-on-fs-change!
  "Simple function to run the needed action on file change"
  [project arg-group tracker modified]
  (println "We have a mofidication" modified)
  (try 
    (main/resolve-and-apply project arg-group)
    (catch Exception e
      (str "Caught Exception " (.getMessage e)))))


(defn- call-react-fns
  "We may have lots of react-fns so tha fn just calls each of them"
  [fns tracker modified]
  (doseq [f fns]
    (f tracker modified)))


(defn- prepare-react-fns [project args]
  (for [arg-group (group-args args)]
          (partial react-on-fs-change! project arg-group)))


(defn ^:no-project-needed ^:higher-order fschange
  "Higher-order task to perform other tasks in succession on file change.

Each comma-separated group should be a task name followed by optional arguments.

USAGE: lein do test, compile :all, deploy private-repo"
  [project & args]
  (let [react-fns (prepare-react-fns project (rest args))
        react-fn (partial call-react-fns react-fns)
        files [(first args)]]
    (println (rest args))
    (println "To Check " files)
    (schedule
     :autotest
     #(scan-and-react!
        current-tracker
        {:files files}
        scan
        :react-fn react-fn)
     1000)
    (while true
      (Thread/sleep 10000))))


