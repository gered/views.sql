(ns views.sql.vexec-tests
  (:use
    clojure.test
    views.sql.test-fixtures
    views.sql.core)
  (:require
    [clojure.java.jdbc :as jdbc]
    [views.core :as views]))

(defn vexec-redefs-fixture [f]
  (reset! redefs-called {})
  (with-redefs
    [jdbc/query       (->redef-fn :jdbc/query :jdbc/query-return-value)
     jdbc/execute!    (->redef-fn :jdbc/execute! :jdbc/execute!-return-value)
     views/put-hints! (->redef-fn :views/put-hints!)]
    (f)))


(use-fixtures :each clear-query-cache-fixture reset-test-view-system-fixture vexec-redefs-fixture)

(deftest vexec-runs-query-and-puts-hints
  (let [sqlvec ["INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL)"]
        result (vexec! test-view-system test-db sqlvec)]
    (is (= [test-db sqlvec] (:jdbc/execute! @redefs-called)))
    (is (= :jdbc/execute!-return-value result))
    (is (called-with-args? :views/put-hints! test-view-system [(views/hint nil #{:example} hint-type)]))))

(deftest vexec-runs-query-with-returning-clause
  (let [sqlvec ["INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL) RETURNING *"]
        result (vexec! test-view-system test-db sqlvec)]
    (is (= [test-db sqlvec] (:jdbc/query @redefs-called)))
    (is (= :jdbc/query-return-value result))
    (is (called-with-args? :views/put-hints! test-view-system [(views/hint nil #{:example} hint-type)]))))

(deftest namespace-is-passed-along-to-hints-via-vexec
  (let [sqlvec ["INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL)"]
        result (vexec! test-view-system test-db sqlvec {:namespace :foobar})]
    (is (= [test-db sqlvec] (:jdbc/execute! @redefs-called)))
    (is (= :jdbc/execute!-return-value result))
    (is (called-with-args? :views/put-hints! test-view-system [(views/hint :foobar #{:example} hint-type)]))))

(deftest manually-provided-hints-to-vexec-are-passed-to-views-system
  (let [sqlvec ["INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL)"]
        result (vexec! test-view-system test-db sqlvec [:foo :bar])]
    (is (= [test-db sqlvec] (:jdbc/execute! @redefs-called)))
    (is (= :jdbc/execute!-return-value result))
    (is (called-with-args? :views/put-hints! test-view-system [(views/hint nil #{:foo :bar} hint-type)]))))

(deftest manually-provided-hints-to-vexec-also-requires-specifying-returning-option
  (let [sqlvec ["INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL) RETURNING *"]
        result (vexec! test-view-system test-db sqlvec [:foo :bar])]
    ; means jdbc/execute! was called which is correct behaviour since the returning option was not specified.
    ; if jdbc/execute! was really called it would throw an exception since the INSERT query used has a RETURNING clause
    (is (= [test-db sqlvec] (:jdbc/execute! @redefs-called)))
    (is (= :jdbc/execute!-return-value result))
    (is (called-with-args? :views/put-hints! test-view-system [(views/hint nil #{:foo :bar} hint-type)])))
  ; manually reset some things
  (reset! redefs-called {})
  (reset! query-info-cache {})
  (let [sqlvec ["INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL) RETURNING *"]
        result (vexec! test-view-system test-db sqlvec [:foo :bar] {:returning? true})]
    (is (= [test-db sqlvec] (:jdbc/query @redefs-called)))
    (is (= :jdbc/query-return-value result))
    (is (called-with-args? :views/put-hints! test-view-system [(views/hint nil #{:foo :bar} hint-type)]))))
