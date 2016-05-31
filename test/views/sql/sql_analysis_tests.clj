(ns views.sql.sql-analysis-tests
  (:use
    clojure.test
    views.sql.test-fixtures
    views.sql.core))

(use-fixtures :each clear-query-cache-fixture)


;; NOTE: the purpose of the tests which look at analysis results of SQL queries
;;       is __NOT__ to test how good the support of JSqlParser is, but just
;;       to ensure that the views.sql abstraction over it works.

(deftest select-query-analyzed-correctly
  (let [info (query-info "SELECT * FROM book WHERE price > 100.00 ORDER BY title")]
    (is (= :select (:type info)))
    (is (= true (:returning? info)))
    (is (= #{:book} (:tables info)))))

(deftest insert-query-analyzed-correctly
  (let [info (query-info "INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL)")]
    (is (= :insert (:type info)))
    (is (= false (:returning? info)))
    (is (= #{:example} (:tables info)))))

(deftest insert-query-with-returning-clause-analyzed-correctly
  (let [info (query-info "INSERT INTO example (field1, field2, field3) VALUES ('test', 'N', NULL) RETURNING *")]
    (is (= :insert (:type info)))
    (is (= true (:returning? info)))
    (is (= #{:example} (:tables info)))))

(deftest update-query-analyzed-correctly
  (let [info (query-info "UPDATE example SET field1 = 'updated value' WHERE field2 = 'N'")]
    (is (= :update (:type info)))
    (is (= false (:returning? info)))
    (is (= #{:example} (:tables info)))))

(deftest delete-query-analyzed-correctly
  (let [info (query-info "DELETE FROM example WHERE field2 = 'N'")]
    (is (= :delete (:type info)))
    (is (= false (:returning? info)))
    (is (= #{:example} (:tables info)))))

(deftest unsupported-query-types-throw-exception
  (is (thrown? Exception (query-info "ALTER TABLE example ADD column4 NUMBER(3) NOT NULL"))))

(deftest multiple-query-tables-are-included-correctly
  (let [info (query-info "SELECT * FROM book JOIN publisher ON book.publisher_id = publisher.id WHERE price > 100.00 ORDER BY title")]
    (is (= :select (:type info)))
    (is (= true (:returning? info)))
    (is (= #{:book :publisher} (:tables info)))))
