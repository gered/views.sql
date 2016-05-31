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

(def redefs-called (atom {}))

(defn ->redef-fn
  [name & [return-value]]
  (fn [& args]
    (swap! redefs-called assoc name (vec args))
    return-value))

(defn called-with-args?
  [fn-name-kw & args]
  (= (get @redefs-called fn-name-kw)
     (vec args)))

(defn not-called?
  [fn-name-kw]
  (not (contains? @redefs-called fn-name-kw)))
