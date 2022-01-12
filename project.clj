(defproject gered/views.sql "0.2-SNAPSHOT"
  :description  "Plain SQL view implementation for views"
  :url          "https://github.com/gered/views.sql"

  :license      {:name "MIT License"
                 :url "http://opensource.org/licenses/MIT"}

  :dependencies [[com.github.jsqlparser/jsqlparser "4.3"]
                 [net.gered/views "1.6-SNAPSHOT"]
                 [org.clojure/tools.logging "1.2.4"]]

  :profiles     {:provided
                 {:dependencies [[org.clojure/clojure "1.10.3"]
                                 [org.clojure/java.jdbc "0.7.12"]]}

                 :test
                 {:dependencies [[pjstadig/humane-test-output "0.11.0"]]
                  :injections   [(require 'pjstadig.humane-test-output)
                                 (pjstadig.humane-test-output/activate!)]}})
