(ns views.sql.view-test
  (:use
    clojure.test
    views.sql.test-fixtures
    views.sql.core
    views.sql.view
    views.protocols)
  (:require
    [clojure.java.jdbc :as jdbc]
    [views.core :as views]))

(defn view-redefs-fixture [f]
  (reset! redefs-called {})
  (with-redefs
    [jdbc/query       (->redef-fn :jdbc/query :jdbc/query-return-value)
     views/put-hints! (->redef-fn :views/put-hints!)]
    (f)))


(use-fixtures :each clear-query-cache-fixture reset-test-view-system-fixture view-redefs-fixture)

(deftest basic-sql-view-works
  (let [sqlvec   ["SELECT * FROM foobar"]
        sql-fn   (fn [] sqlvec)
        sql-view (view :test-view test-db sql-fn)]
    (is (satisfies? IView sql-view))
    (is (= :test-view (id sql-view)))
    (is (= true (relevant? sql-view nil [] [(views/hint nil #{:foobar} hint-type)])))
    (is (= false (relevant? sql-view nil [] [(views/hint nil #{:baz} hint-type)])))
    (is (= :jdbc/query-return-value (data sql-view nil [])))
    (is (called-with-args? :jdbc/query test-db sqlvec {}))))

(deftest basic-sql-view-works-with-parameters
  (let [sqlvec   ["SELECT * FROM foobar"]
        sql-fn   (fn [a b] (into sqlvec [a b]))
        sql-view (view :test-view test-db sql-fn)]
    (is (= true (relevant? sql-view nil [1 2] [(views/hint nil #{:foobar} hint-type)])))
    (is (= :jdbc/query-return-value (data sql-view nil [1 2])))
    (is (called-with-args? :jdbc/query test-db (into sqlvec [1 2]) {}))))

(deftest basic-sql-view-works-with-namespace
  (let [sqlvec   ["SELECT * FROM foobar"]
        sql-fn   (fn [] sqlvec)
        sql-view (view :test-view test-db sql-fn)]
    (is (= true (relevant? sql-view :abc [] [(views/hint :abc #{:foobar} hint-type)])))
    (is (= false (relevant? sql-view :123 [] [(views/hint :abc #{:foobar} hint-type)])))
    (is (= :jdbc/query-return-value (data sql-view nil [])))
    (is (called-with-args? :jdbc/query test-db sqlvec {}))))

(deftest view-db-fn-is-used-when-provided
  (let [alt-test-db {:alternate-test-db-conn true}
        db-fn       (fn [namespace] alt-test-db)
        sqlvec      ["SELECT * FROM foobar"]
        sql-fn      (fn [] sqlvec)
        sql-view    (view :test-view db-fn sql-fn)]
    (is (= :jdbc/query-return-value (data sql-view nil [])))
    (is (called-with-args? :jdbc/query alt-test-db sqlvec {}))))

(deftest view-db-fn-is-passed-namespace
  (let [test-namespace :test-namespace
        alt-test-db    {:alternate-test-db-conn true}
        db-fn          (fn [namespace]
                         (is (= namespace :test-namespace))
                         alt-test-db)
        sqlvec         ["SELECT * FROM foobar"]
        sql-fn         (fn [] sqlvec)
        sql-view       (view :test-view db-fn sql-fn)]
    (is (= :jdbc/query-return-value (data sql-view test-namespace [])))
    (is (called-with-args? :jdbc/query alt-test-db sqlvec {}))))

(deftest manually-specified-view-hints-are-used-correctly
  (with-redefs
    [query-tables (->redef-fn :query-tables)]
    (let [sqlvec   ["SELECT * FROM foobar"]
          sql-fn   (fn [] sqlvec)
          sql-view (view :test-view test-db sql-fn [:foo :bar])]
      (is (= false (relevant? sql-view nil [] [(views/hint nil #{:foobar} hint-type)])))
      (is (= true (relevant? sql-view nil [] [(views/hint nil #{:foo} hint-type)])))
      (is (= true (relevant? sql-view nil [] [(views/hint nil #{:bar} hint-type)])))
      (is (= true (relevant? sql-view nil [] [(views/hint nil #{:bar} hint-type)
                                              (views/hint nil #{:foo} hint-type)])))
      (is (not-called? :query-tables)))))

(deftest row-and-result-set-fns-are-passed-to-jdbc
  (let [row-fn        (fn [row] row)
        result-set-fn (fn [results] results)
        sqlvec        ["SELECT * FROM foobar"]
        sql-fn        (fn [] sqlvec)
        sql-view      (view :test-view test-db sql-fn {:row-fn        row-fn
                                                       :result-set-fn result-set-fn})]
    (is (satisfies? IView sql-view))
    (is (= :test-view (id sql-view)))
    (is (= true (relevant? sql-view nil [] [(views/hint nil #{:foobar} hint-type)])))
    (is (= false (relevant? sql-view nil [] [(views/hint nil #{:baz} hint-type)])))
    (is (= :jdbc/query-return-value (data sql-view nil [])))
    (is (called-with-args? :jdbc/query test-db sqlvec {:row-fn        row-fn
                                                       :result-set-fn result-set-fn}))))