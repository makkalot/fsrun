(ns com.makkalot.testrun
  (:require [clojure.java.io :as io]
            [clojure.java.shell  :refer [sh]]
            [me.raynes.fs :as fs])
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
(defn create-tracker
  "We keep here the files we track"
  []
  {})

(defn create-tracker-atom
  "Atom based tracker"
  []
  (atom (create-tracker)))

(def current-tracker (atom (create-tracker)))


(defn get-file-list
  "Fn takes a list of files/globbing paths and
   Returns back a sequence of ^File objects
   Usage : (get-file-list [\"/file/path\" \"/path/*.js\"])
   "
  [file-options]
  (map #(.getCanonicalFile ^File %)
       (reduce (fn [files cur-entry]
                 (let [cur-file (io/file cur-entry)]
                   (if (.exists ^File cur-file)
                     (cons cur-file files)
                     (concat files (fs/glob cur-entry)))))
               [] file-options)))


(defn modified-files
  "Fn checks the tracker dict against supplied
   file list and returns back only modified ones
   @param tracker : tracker dict
   @param files : A list of ^File objects
   @returns : Only modified list of ^File"
  [tracker files]
  (filter #(< (::time tracker 0) (.lastModified ^File %)) files))

(defn update-files
  "Fn updates tracker map with the modified list of Files"
  [tracker modified]
  (let [now (System/currentTimeMillis)]
    (-> tracker
        (update-in [::files] (fnil into #{}) modified)
        (assoc ::time now))))

(defn get-tracker-time [tracker]
  (::time tracker))


(defn get-tracker-files [tracker]
  (::files tracker))

(defn scan
  "Fn scans the supplied paths they can be
   real paths or globbing paths and checks those
   paths against given tracker and returns back only
   modified ones back as a sequence"
  [tracker dirs]
  (let [ds (seq dirs)
        files (get-file-list ds)]
    (seq (modified-files tracker files))))


(defn react-on-change!
  "A default implementatiton of callback
   that will be called on file change. You should
   supply your own it is a default one and empty for now
   Accepts tracker which is Map like object and a list of
   modified files to act on."
  [tracker modified])


(defn scan-and-react!
  "Main function that checks :files field of supplied
  options and calls the react-fn on file change and updates
  the current tracker file map with new modified files
  Should be called in some loop!"
  [tracker options scanner & {:keys [react-fn] :or {react-fn react-on-change!}}]
  (swap! tracker
       #(let [modified (scanner % (:files options))]
         (if modified
           (do
             (react-fn % modified)
             (update-files % modified))
           %))))
