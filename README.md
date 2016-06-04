# views.sql

SQL plugin for the [views][1] library. Allows for views to be created 
which retrieve data via [clojure.java.jdbc][2] using SQL queries 
provided as plain ol' strings (as opposed to [views.honeysql][3] which 
uses HoneySQL-format SQL queries). Provides an alternate 
`execute!`-like function to execute INSERT/UPDATE/DELETE queries and 
add appropriate hints to the view system at the same time to trigger 
view refreshes.

[1]: https://github.com/gered/views
[2]: https://github.com/clojure/java.jdbc
[3]: https://github.com/gered/views.honeysql

views.sql interops well with views.honeysql when both types of views 
are included within the same system.


## Leiningen

```clj
[gered/views.sql "0.1.0-SNAPSHOT"]
```


## Creating SQL Views

```clj
(require '[views.core :as views]
         '[views.sql.view :as vsql])

(def db ... )            ; a standard JDBC database connection map
(def view-system ... )   ; pre-initialized view system


; view functions. these are just functions that return SQL in a "sql vector" format.
; (which is the exact same format as you pass SQL in to clojure.java.jdbc/query, etc)

(defn my-view-sql []
  ["SELECT * FROM foo"])

(defn people-by-type-sql [type]
  ["SELECT first_name, last_name FROM people WHERE type = ?" type])


; add 2 views, :my-view and :people-by-type, to the view system

(views/add-views!
  view-system
  [(vsql/view :my-view db my-view-sql)
   (vsql/view :people-by-type db people-by-type-sql)])
```

The calls to `views.sql.view/view` return instances of a `SQLView` 
view. The "view functions" which contain the actual SQL queries are 
called in two instances:

* When the view's data is being refreshed. The view function is called
to get the SQL to be run via `clojure.java.jdbc/query` using the `db`
connection that was provided to the view.
* Whenever hints are being checked for relevancy against the view when
the view system is determining whether the view needs to be refreshed
or not.

Note also that the view functions can take any number of parameters
which are provided during view subscription:

```clj
(require '[views.core :refer [subscribe! ->view-sig]])

(subscribe! view-system (->view-sig :my-namespace :my-view []) 123 nil)
(subscribe! view-system (->view-sig :my-namespace :people-by-type ["student"]) 123 nil)
```

### Extra Features and Options

You can use clojure.java.jdbc's `:row-fn` and `:result-set-fn` (see
[here][4] and [here][5] for more info on what these options are) with
SQL views:

```clj
(vsql/view :foobar-view db foobar-view-sql {:row-fn my-row-fn 
                                            :result-set-fn my-result-set-fn})
```

[4]: http://clojure-doc.org/articles/ecosystem/java_jdbc/using_sql.html#processing-each-row-lazily
[5]: http://clojure-doc.org/articles/ecosystem/java_jdbc/using_sql.html#processing-a-result-set-lazily

Additionally the `db` argument can be a function that accepts a
namespace and returns a standard database connection map.

```clj
(defn db-selector [namespace]
  (case namespace
    :foo foo-db
    :bar bar-db
    default-db))

(vsql/view :people-by-type db-selector people-by-type-sql)
```

In this case, `db-selector` would be called only when the view data is
being refreshed (it is not used during hint relevancy checks). The
namespace that would be passed in is taken from the view
subscription(s) for which the view is being refresh for (so it could
be anything, even `nil`... whatever was provided as the namespace at
the time subscriptions are created).


## Running INSERT/UPDATE/DELETE Queries

Instead of using clojure.java.jdbc's `execute!` or `query!`, you
should instead use `views.sql.core/vexec!`:

```clj
(require '[views.sql.core :refer [vexec!]])

(vexec! view-system db
        ["INSERT INTO people (type, first_name, last_name) VALUES (?, ?, ?)"
         "student" "Foo" "Bar"])
```

This will both, execute the SQL query and also analyze it to determine
what hints need to be added to the view system and then add them.

With the above `vexec!` call the hints that would be added to the view
system would trigger view refreshes for anyone subscribed to any SQL
views in the system that use a SELECT query to retrieve data from the
"people" table (either using another simple SELECT, or JOINing it with
other tables as part of a larger query, a sub-SELECT, etc).

### Transactions

If you need to run some SQL queries within a transaction, you should
use `views.sql.core/with-view-transaction` instead of 
clojure.java.jdbc's `with-db-transaction`. It basically works exactly
the same:

```clj
(require '[views.sql.core :refer [with-view-transaction]])

(with-view-transaction
  view-system            ; need to pass in the view-system atom
  [dt db]
  (let [user-id (vexec! view-system dt 
                        ["INSERT INTO users (username) 
                          VALUES (?) 
                          RETURNING user_id"
                         "fbar"])]
    (vexec! view-system dt
            ["INSERT INTO people (type, first_name, last_name, user_id) 
              VALUES (?, ?, ?, ?)"
             "student" "Foo" "Bar" user-id])))
```

The hints generated by any `vexec!` calls within a transaction are
collected in a list and only at the end of the (successful) transaction
are they added to the view system.

### Namespaces

Namespaces can be specified in an additional options map as the last
argument to `vexec!`. If you don't provide this, then a `nil` namespace
is used for the hints sent to the view system.

```clj
(vexec! view-system db
        ["INSERT INTO people (type, first_name, last_name) VALUES (?, ?, ?)"
         "student" "Foo" "Bar"]
        {:namespace :my-namespace)
```


## Hints

Hints for the view system are automatically determined from the SQL
queries being used in the view functions and from `vexec!` calls by
analyzing the SQL and figuring out what tables are being queried from
or changed. All you need to do is write the SQL.

The hints themselves are simply SQL table names represented as 
keywords, e.g. `:people` for the "people" table. Hints are considered
relevant to a SQL view if the list of tables being queried from in the
view's SELECT statement have at least some matches against the hints
being compared against.

This SQL query analysis is done via [JSqlParser][6]. The tables list
obtained from the analysis is cached by views.sql so as to keep
performance as fast as possible.

[6]: https://github.com/JSQLParser/JSqlParser

> Hints generated by views.sql are compatible with the hints generated
> by [views.honeysql][7], so you can easily mix-and-match these views
> within the same system and get view refreshes triggered as you would
> expect for both types of views.

[7]: https://github.com/gered/views.honeysql

### When a SQL Query Cannot Be Analyzed

While [JSqlParser][8] is quite a capable library and is able to
correctly parse many queries without any problems, it is unfortunately
not perfect (sad, but true). You may eventually run into some SQL query
that it cannot parse and an exception gets thrown during a view refresh
or a call to `vexec!`.

[8]: https://github.com/JSQLParser/JSqlParser

You can work around this by manually specifying hints appropriate to
the problematic SQL query in your view definition or `vexec!` call.
Doing this will skip the use of JSqlParser entirely for the problem
query.

```clj
; manually specifying the hints: the list of tables this query uses
(vsql/view :people-by-type db people-by-type-sql [:people])


; and also for vexec! ...
(vexec! view-system db
        ["INSERT INTO people (type, first_name, last_name) VALUES (?, ?, ?)"
         "student" "Foo" "Bar"]
        [:people])
```

> Be careful when doing this! If you later update your SQL query, make
> sure you also update the list of hints since they are not being
> determined automatically.

JSqlParser is actively maintained however, so the number of cases where
manual hints need to be provided should decrease in the future. Usually
you will only run into this if you're using something outside of ANSI
SQL (e.g. vendor-specific extensions, but even some of these are 
supported).

Another alternative you may wish to consider is using 
[views.honeysql][9] for these problematic queries. Due to HoneySQL
representing SQL queries as Clojure data structures, when it comes time
to analyzing queries to get the hints it doesn't run into the same SQL
parsing problems that JSqlParser sometimes does.

[9]: https://github.com/gered/views.honeysql

#### Use of the RETURNING Clause with Problematic SQL Queries

This only pertains to `vexec!` of course, but if you need to run an 
INSERT/UPDATE/DELETE query which JSqlParser cannot parse and are forced
to manually specify hints, you will also need to provide the 
`:returning?` option if the query uses a RETURNING clause.

When you are forced to manually specify hints, it is because JSqlParser
was unable to parse the query. This also means we cannot rely on it's
automatic detection of a RETURNING clause so we need to help out 
`vexec!` by providing this information:

```clj
(vexec! view-system db
        ["INSERT INTO mytable (field1, field2, field3) 
          VALUES ('abc', 'xyz', 123) 
          RETURNING *"]
        [:mytable]
        {:returning? true})
```

> `vexec!` uses `clojure.java.jdbc/query` to run queries that have a 
> RETURNING claus, and `clojure.java.jdbc/execute!` to run those 
> without. Thus the need for `vexec!` to know about the presence of a 
> RETURNING clause.


## License

Copyright © 2016 Gered King

Distributed under the MIT License.
