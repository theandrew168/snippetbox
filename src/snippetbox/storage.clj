(ns snippetbox.storage
  (:require [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]))

(defn connect [db]
  (jdbc.date-time/read-as-instant)
  (jdbc/get-datasource db))

(defn snippet-create [conn {:keys [title content expires]}]
  (let [now (jt/instant)
        expires (jt/plus now (jt/days expires))]
    (jdbc/execute-one!
     conn
     ["INSERT INTO snippet (title, content, created, expires)
       VALUES (?, ?, ?, ?)
       RETURNING id"
      title content now expires])))

(defn snippet-list [conn at n]
  (jdbc/execute!
   conn
   ["SELECT * FROM snippet WHERE expires > ? ORDER BY created DESC LIMIT ?" at n]))

(defn snippet-read [conn id at]
  (jdbc/execute-one!
   conn
   ["SELECT * FROM snippet WHERE id = ? AND expires > ?" id at]))

(defn snippet-update [conn {:keys [id title content]}]
  (jdbc/execute-one!
   conn
   ["UPDATE snippet
     SET title = ?, content = ?
     WHERE id = ?"
    title content id]))

(defn snippet-delete [conn id]
  (jdbc/execute-one!
   conn
   ["DELETE FROM snippet WHERE id = ?" id]))

(comment

  ;; connect to database
  (def conn (connect "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"))

  (snippet-create conn {:title "Foo" :content "A tale about foo" :expires 7})
  (snippet-list conn (jt/instant) 3)
  (snippet-read conn 1 (jt/instant))
  (snippet-update conn {:id 1 :title "Bar" :content "update the content"})
  (snippet-delete conn 1)

  :rcf)