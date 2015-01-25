(defproject cljs-flux "0.1.0"
  :description "ClojureScript implementation of Facebook's Flux architecture"
  :url "https://github.com/kgann/cljs-flux"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2727"]]

  :hooks [leiningen.cljsbuild]

  :plugins [[com.cemerick/clojurescript.test "0.3.3"]
            [lein-cljsbuild "1.0.4"]]

  :profiles {:dev {:source-paths ["dev"]
                   :plugins [[com.cemerick/austin "0.1.6"]]}}

  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" :runner "target/testable.js"]}
              :builds {:test {:source-paths ["src" "test"]
                              :compiler {:output-to "target/testable.js"
                                         :optimizations :whitespace}}
                       :dev {:source-paths ["src"]
                             :compiler {:output-to "target/cljs_flux.js"
                                        :optimizations :none}}}})
