(ns views.sql.view
  (:require
    [views.protocols :refer [IView]]
    [views.sql.core :refer [hint-type query-tables]]
    [clojure.set :refer [intersection]]
    [clojure.java.jdbc :as jdbc]
    [clojure.tools.logging :refer [warn]]))

; this implementation based on views-honeysql

(defrecord SQLView [id db-or-db-fn query-fn options]
  IView
  (id [_] id)
  (data [_ namespace parameters]
    (let [db    (if (fn? db-or-db-fn)
                  (db-or-db-fn namespace)
                  db-or-db-fn)
          start (System/currentTimeMillis)
          data  (jdbc/query db
                            (apply query-fn parameters)
                            (select-keys options [:row-fn :result-set-fn]))
          time  (- (System/currentTimeMillis) start)]
      (when (>= time 1000) (warn id "took" time "msecs"))
      data))
  (relevant? [_ namespace parameters hints]
    (let [[sql & _] (apply query-fn parameters)
          tables    (if (:parse? options)
                      (query-tables sql)
                      (:tables options))
          nhints    (filter #(and (= namespace (:namespace %))
                                  (= hint-type (:type %))) hints)]
      (boolean (some #(not-empty (intersection (:hint %) tables)) nhints)))))

(defn- view*
  [id db-or-db-fn sql-fn {:keys [row-fn result-set-fn tables]
                          :or   {row-fn        identity
                                 result-set-fn doall}
                          :as   options}]
  (SQLView. id db-or-db-fn sql-fn options))

(defn view
  "Creates a SQL view that uses a JDBC database configuration.

   Arguments are:
   - id: an id for this view that is unique within the view system
   - db-or-db-fn: either a database connection map, or a function that will get
                  passed a namespace and should return a database connection map
   - sql-fn: a function that returns a JDBC-style vector containing a SELECT
             query followed by any parameters. this query will be run whenever
             this view needs to be refreshed.

   Options are:
   - row-fn: a function that if specified will be run against each row in the
             view's result set before returning it.
   - result-set-fn: a function that will be run against the entire view's result
                    set before returning it.

   NOTE:
   If the SQL being run cannot be parsed (e.g. due to use of database-specific
   extensions, or other limitations of JSqlParser), you will need to manually
   specify the list of table names (as keywords) that the SQL query will affect
   as the optional tables argument."
  {:arglists '([id db-or-db-fn sql-fn & options]
               [id db-or-db-fn sql-fn tables & options])}
  [id db-or-db-fn sql-fn & options]
  (let [[first-option & [other-options]] options]
    (if (or (nil? options)
            (map? first-option))
      (view* id db-or-db-fn sql-fn (assoc first-option :parse? true))
      (view* id db-or-db-fn sql-fn (assoc other-options
                                     :tables (set first-option)
                                     :parse? false)))))
