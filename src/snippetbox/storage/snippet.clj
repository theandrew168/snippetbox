(ns snippetbox.storage.snippet
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as s]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]))

(defprotocol SnippetStorage
  (create [_ snippet])
  (read-by-id [_ id at])
  (list-recent [_ n at]))

(defn- select-by-id [id at]
  (-> (s/select :id :title :content :created :expires)
      (s/from :snippet)
      (s/where [:= :snippet.id id])
      (s/where [:> :snippet.expires at])
      (sql/format)))

(defn- select-recent [n at]
  (-> (s/select :id :title :content :created :expires)
      (s/from :snippet)
      (s/where [:> :snippet.expires at])
      (s/order-by [:snippet.created :desc])
      (s/limit n)
      (sql/format)))

(defn- insert [snippet]
  (-> (s/insert-into :snippet)
      (s/values [snippet])
      (s/returning :id)
      (sql/format)))

(deftype PostgreSQLSnippetStorage [conn]
  SnippetStorage
  (create [_ snippet] (jdbc/execute-one! conn (insert snippet)))
  (read-by-id [_ id at] (jdbc/execute-one! conn (select-by-id id at)))
  (list-recent [_ n at] (jdbc/execute! conn (select-recent n at))))

(comment

  (insert {:title "asdf" :content "wow" :created (jt/instant) :expires (jt/instant)})
  (select-by-id 4 (jt/instant))
  (select-recent 3 (jt/instant))

  (def conn "jdbc:postgresql://postgres:postgres@localhost:5432/postgres")
  (def storage (->PostgreSQLSnippetStorage conn))

  (create storage {:title "Foo"
                   :content "A tale about foo"
                   :created (jt/instant)
                   :expires (jt/instant)})
  (read-by-id storage 32 (jt/instant))
  (list-recent storage 3 (jt/instant))

  :rcf)