(ns snippetbox.storage
  (:require [integrant.core :as ig]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]))

(defmethod ig/init-key :app/database [_ {:keys [connection-string]}]
  (jdbc.date-time/read-as-instant)
  (jdbc/get-datasource connection-string))

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

  (def system
    (-> "config.edn"
        slurp
        ig/read-string
        (ig/init [:app/database])))

  (ig/halt! system [:app/database])

  (snippet-create (:app/database system) {:title "Foo" :content "A tale about foo" :expires 7})
  (snippet-list (:app/database system) (jt/instant) 3)
  (snippet-read (:app/database system) 1 (jt/instant))
  (snippet-update (:app/database system) {:id 1 :title "Bar" :content "update the content"})
  (snippet-delete (:app/database system) 1)

  :rcf)