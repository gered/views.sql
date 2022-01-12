(defproject net.gered/views.sql "0.3.0-SNAPSHOT"
  :description         "Plain SQL view implementation for views"
  :url                 "https://github.com/gered/views.sql"
  :license             {:name "MIT License"
                        :url "http://opensource.org/licenses/MIT"}

  :dependencies        [[com.github.jsqlparser/jsqlparser "4.3"]
                        [net.gered/views "1.6.0"]
                        [org.clojure/tools.logging "1.2.4"]]

  :profiles            {:provided
                        {:dependencies [[org.clojure/clojure "1.10.3"]
                                        [org.clojure/java.jdbc "0.7.12"]]}

                        :test
                        {:dependencies [[pjstadig/humane-test-output "0.11.0"]]
                         :injections   [(require 'pjstadig.humane-test-output)
                                        (pjstadig.humane-test-output/activate!)]}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]

  :release-tasks       [["vcs" "assert-committed"]
                        ["change" "version" "leiningen.release/bump-version" "release"]
                        ["vcs" "commit"]
                        ["vcs" "tag" "v" "--no-sign"]
                        ["deploy"]
                        ["change" "version" "leiningen.release/bump-version"]
                        ["vcs" "commit" "bump to next snapshot version for future development"]
                        ["vcs" "push"]]

  )
