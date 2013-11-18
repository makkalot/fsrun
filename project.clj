(defproject testrun "0.1.0"
  :description "Plugin to run cljs tests on change"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :eval-in-leiningen true
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1934"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/algo.monads "0.1.4"]]
  

  :plugins [[com.cemerick/clojurescript.test "0.2.1"]
            [lein-cljsbuild "1.0.0-alpha1"]]

   :cljsbuild {:builds
                {:dev
                  {;; clojurescript source code path
                  :source-paths ["src/brepl" "src/cljs" "test/cljs"] ;;; added "src/brepl"

                  ;; Google Closure Compiler options
                  :compiler {;; the name of emitted JS script file
                             :output-to "resources/public/js/modern.js"

                             ;; minimum optimization
                             :optimizations :whitespace
                             ;; prettyfying emitted JS
                             :pretty-print true}}}

              :test-commands 
              {"unit" ["phantomjs" :runner "resources/public/js/modern.js"]}})



