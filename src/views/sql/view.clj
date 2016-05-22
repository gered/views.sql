(ns views.sql.view
  (:require
    [views.protocols :refer [IView]]
    [views.sql.core :refer [hint-type query-tables]]
    [clojure.set :refer [intersection]]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :refer [warn]]))

; this implementation based on views-honeysql

(defrecord SQLView [id db-or-db-fn query-fn row-fn]
  IView
  (id [_] id)
  (data [_ namespace parameters]
    (let [db    (if (fn? db-or-db-fn)
                  (db-or-db-fn namespace)
                  db-or-db-fn)
          start (System/currentTimeMillis)
          data  (jdbc/query db (apply query-fn parameters) {:row-fn row-fn})
          time  (- (System/currentTimeMillis) start)]
      (when (>= time 1000) (warn id "took" time "msecs"))
      data))
  (relevant? [_ namespace parameters hints]
    (let [[sql & sqlparams] (apply query-fn parameters)
          tables            (query-tables sql)
          nhints            (filter #(and (= namespace (:namespace %))
                                          (= hint-type (:type %))) hints)]
      (boolean (some #(not-empty (intersection (:hint %) tables)) nhints)))))

(defn view
  "Creates a SQL view that uses a JDBC database configuration.

  db-or-db-fn - either a database connection map, or a function that will get passed
                a namespace and should return a database connection map
  sql-fn      - a function that returns a JDBC-style vector containing a SELECT query
                followed by any parameters. this query will be run whenever this view
                needs to be refreshed."
  [id db-or-db-fn sql-fn & {:keys [row-fn]}]
  (SQLView. id db-or-db-fn sql-fn (or row-fn identity)))