(ns snippetbox.postgresql
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as s]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [snippetbox.storage :as storage]))

(defn- create-snippet-query [snippet]
  (-> (s/insert-into :snippet)
      (s/values [snippet])
      (s/returning :id)
      (sql/format)))

(defn- read-snippet-by-id-query [id at]
  (-> (s/select :id :title :content :created :expires)
      (s/from :snippet)
      (s/where [:= :snippet.id id])
      (s/where [:> :snippet.expires at])
      (sql/format)))

(defn- list-recent-snippets-query [n at]
  (-> (s/select :id :title :content :created :expires)
      (s/from :snippet)
      (s/where [:> :snippet.expires at])
      (s/order-by [:snippet.created :desc])
      (s/limit n)
      (sql/format)))

(defrecord PostgreSQLStorage [conn]
  storage/Storage
  (create-snippet [_ snippet] (jdbc/execute-one! conn (create-snippet-query snippet)))
  (read-snippet-by-id [_ id at] (jdbc/execute-one! conn (read-snippet-by-id-query id at)))
  (list-recent-snippets [_ n at] (jdbc/execute! conn (list-recent-snippets-query n at))))

(defn store [conn]
  (map->PostgreSQLStorage {:conn conn}))

(comment

  (create-snippet-query {:title "asdf" :content "wow" :created (jt/instant) :expires (jt/instant)})
  (read-snippet-by-id-query 4 (jt/instant))
  (list-recent-snippets-query 3 (jt/instant))

  (def conn "jdbc:postgresql://postgres:postgres@localhost:5432/postgres")
  (def store (map->PostgreSQLStorage {:conn conn}))

  (storage/create-snippet store {:title "Foo"
                                 :content "A tale about foo"
                                 :created (jt/instant)
                                 :expires (jt/instant)})
  (storage/read-snippet-by-id store 32 (jt/instant))
  (storage/list-recent-snippets store 3 (jt/instant))

  :rcf)
