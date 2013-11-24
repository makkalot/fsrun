(ns com.makkalot.testrun.test-auto
  (:require [me.raynes.fs :as fs]
            [clojure.string :refer [join]]
            [com.makkalot.testrun :refer
             [create-tracker modified-files
              update-files get-tracker-time get-tracker-files
              scan scan-and-react! create-tracker-atom find-files]])
  
  (:use clojure.test)
                     
  (:import [java.io File]))


(defn- left-strip-path [^String s]
  (if (.startsWith s File/separator)
    (apply str (rest s))
    s))

(defn- right-strip-path [^String s]
  (if (.endsWith s File/separator)
    (apply str (butlast s))
    s))


(defn- strip-path [^String s]
  (let [left (left-strip-path s)
        right (right-strip-path left)]
    right))


(defn- join-path [base-path & paths]
  (let [stripped-paths (map strip-path paths)
        bpath (right-strip-path base-path)]
    (join File/separator (cons bpath stripped-paths))))

(defn- generate-n-js-files [abs-path n]
  (for [i (range n)]
    (let [path (join-path abs-path (str i ".js"))
          f (fs/file path)]
     f)))

(defn- create-n-js-files!
  "You should note that we have here side effects so doseq"
  [abs-path n]
  (let [files (generate-n-js-files abs-path n)]
    (doseq [f files]
      (fs/create f))
    files))


(defn get-file-list
  "Gets all of the files of the directory"
  [dir]
  ;(println "Brinign what ! " dir)
  (let [files (map
               #(join-path dir %1)
               (fs/list-dir dir))]
        files))

(defn js-file?
  "Returns true if the java.io.File represents a normal javascript
  file."
  [^java.io.File file]
  (and (.isFile file)
       (.endsWith (.getName file) ".js")))


(defn find-js-files
  "What it gets is absolute paths of files not directories"
  [dirs]
  (find-files dirs :file-filter-fn js-file?))


;created during fixtures
(declare ^:dynamic cur-files)
(declare ^:dynamic cur-dir)

 
(defn tmp-file-fixture [f]
  (let [tmpdir (fs/temp-dir "testrun")
        abs-path (.getAbsolutePath tmpdir)
        files (create-n-js-files! abs-path 3)]
    (try
      (binding [cur-files files
                cur-dir abs-path]
        (f))
      (finally
        (fs/delete-dir abs-path)))))


(deftest test-find-files
  (let [found-files (find-js-files (get-file-list cur-dir))]
                                        ;check the number of files
    (is (== (count cur-files)
            (count found-files)))
    ;check if files are same
    (is (= (set
            (map #(.getAbsolutePath %) cur-files))
           (set
            (map #(.getAbsolutePath %) found-files))))))




(deftest test-modify
  (let [tracker (create-tracker)
        found-files (find-js-files (get-file-list cur-dir))]
    (is (== (count found-files)
            (count
             (modified-files tracker found-files))))))


(deftest test-modify-update
  (let [tracker (create-tracker)
        found-files (find-js-files (get-file-list cur-dir))
        updated-tracker (update-files tracker found-files)
        to-modify (first found-files)]
    (is (not
         (nil?
          (get-tracker-time updated-tracker))))
    (is (= (count found-files)
           (count
            (get-tracker-files updated-tracker))))
    ;touch the file so we can see if it is detected
    (fs/touch (.getAbsolutePath to-modify)
              (+ 1000 (System/currentTimeMillis)))
   
    (let [re-file (first
                   (find-js-files
                    [(.getAbsolutePath to-modify)]))
          modified-fs (modified-files updated-tracker found-files)]

      (is (== 1 (count modified-fs)))
      (is (= (.getAbsolutePath
              (first modified-fs))
             (.getAbsolutePath to-modify))))))

(defn react-fn-test [mark-atom tracker modified]
  (swap! mark-atom update-in [:files] into modified))

(deftest test-scan
  (let
      [tracker (create-tracker-atom)
       file-list (get-file-list cur-dir)
       to-modify (first file-list)
       react-atom (atom {})]
    
    (is (= (count
            (scan tracker file-list))
           (count file-list)))

                                        ;Lets update the tracker here
    (scan-and-react! tracker {:files file-list} scan)
    (fs/touch to-modify
              (+ 1000 (System/currentTimeMillis)))

    (scan-and-react!
     tracker
     {:files file-list}
     scan
     :react-fn (partial react-fn-test react-atom))
    
                                        ;now check if the file is same
    (is (= to-modify
           (.getAbsolutePath (first (@react-atom :files)))))))



(use-fixtures :each tmp-file-fixture)
