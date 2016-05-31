(ns views.sql.test-fixtures
  (:use
    clojure.test
    views.sql.core))

(def test-db {:not-a-real-db-conn true})

(def test-view-system (atom {}))

(defn clear-query-cache-fixture [f]
  (reset! query-info-cache {})
  (f))

(defn reset-test-view-system-fixture [f]
  (reset! test-view-system {})
  (f))
