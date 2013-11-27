# fsrun

fsrun is a simple high order lein task that run some other tasks when a file modification occurs. There is no need to write your own file notifier plugin anymore :) 

## Usage

Use this for user-level plugins:

Put `[fsrun "0.1.1"]` into the `:plugins` vector of your
`:user` profile, or if you are on Leiningen 1.x do `lein plugin install
testrun 0.1.1`.

Use this for project-level plugins:

Put `[fsrun "0.1.1"]` into the `:plugins` vector of your project.clj.

Originally i've created it to be able to run my clojurescript tests automatically on file change:

    $ lein fschange /compiled/js/file.js cljsbuild test

fsrun accepts also globbing path so you can 

    $ lein fschange /compiled/js/*.js cljsbuild test

## License
Distributed under the Eclipse Public License, the same as Clojure.
