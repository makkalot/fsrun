(ns com.makkalot.testrun
  (:require [clojure.java.io :as io]
            [clojure.java.shell  :refer [sh]])
  (:use [leiningen.core.eval :only [eval-in-project]])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]
           [java.io File]))

;scheduling api
(def scheduled-futures (atom {}))

(defn stop [service-tag]
  (if-let [future (service-tag @scheduled-futures)]
    (.cancel future true)
    (swap! scheduled-futures dissoc service-tag)))

(defn schedule [service-tag function interval]
  (when (@scheduled-futures service-tag)
    (stop service-tag))
  (let [executor (ScheduledThreadPoolExecutor. 1)
        function (bound-fn [] (function))
        future (.scheduleWithFixedDelay executor function 0 interval TimeUnit/MILLISECONDS)]
    (swap! scheduled-futures assoc service-tag future)))

;file operations here
(defn create-tracker []
  {})

(defn create-tracker-atom []
  (atom (create-tracker)))

(def current-tracker (atom (create-tracker)))


(defn find-files [dirs file-filter-fn]
  (->> dirs
       (map io/file)
       (filter #(.exists ^File %))
       (mapcat file-seq)
       (filter file-filter-fn)
       (map #(.getCanonicalFile ^File %))))

(defn js-file?
  "Returns true if the java.io.File represents a normal javascript
  file."
  [^java.io.File file]
  (and (.isFile file)
       (.endsWith (.getName file) ".js")))


(defn find-js-files
  "What it gets is absolute paths of files not directories"
  [dirs]
  (find-files dirs js-file?))


(defn modified-files [tracker files]
  (filter #(< (::time tracker 0) (.lastModified ^File %)) files))

(defn update-files [tracker modified]
  (let [now (System/currentTimeMillis)]
    (-> tracker
        (update-in [::files] (fnil into #{}) modified)
        (assoc ::time now))))

(defn get-tracker-time [tracker]
  (::time tracker))


(defn get-tracker-files [tracker]
  (::files tracker))

(defn scan [tracker dirs]
   (let [ds (seq dirs)
        files (find-js-files ds)
        modified (seq (modified-files tracker files))]
    modified))


(defn react-on-change! [tracker modified])


(defn scan-and-react! [tracker options scanner & {:keys [react-fn] :or {react-fn react-on-change!}}]
  (swap! tracker
       #(let [modified (scanner % (:files options))]
         (if modified
           (do
             (react-fn % modified)
             (update-files % modified))
           %))))


(defn get-file-list [test-options]
  (filter #(.endsWith % ".js") test-options))

(defn react-fn-runner
  "Runs the commands for the modified test suite"
  [test-commands tracker modified]
  (let [result (apply sh test-commands)]
    (println "RESULT " result))
  (println "TEST-COMMANDS " test-commands)
  (println "MODIFIED " modified))

(defn autotest
  "autotest method"
  [test-mode options interval]
  (println "AUTOTEST " test-mode options interval)
  (let [track-files (get-file-list options)]
    (schedule
      :autotest
      #(scan-and-react!
        current-tracker
        {:files track-files}
        scan
        :react-fn (partial react-fn-runner options))
      interval)
    (while true
      (Thread/sleep 15000)
      (println "Checking ...")
      (println (.get (:autotest @scheduled-futures))))))

