(ns snippetbox.query
  (:require [honey.sql :as sql]
            [honey.sql.helpers :as s]
            [java-time.api :as jt]))

(defn select-snippet-by-id [id at]
  (-> (s/select :id :title :content :created :expires)
      (s/from :snippet)
      (s/where [:= :snippet.id id])
      (s/where [:> :snippet.expires at])
      (sql/format)))

(defn select-recent-snippets [n at]
  (-> (s/select :id :title :content :created :expires)
      (s/from :snippet)
      (s/where [:> :snippet.expires at])
      (s/order-by [:snippet.created :desc])
      (s/limit n)
      (sql/format)))

(defn insert-snippet [snippet]
  (-> (s/insert-into :snippet)
      (s/values [snippet])
      (s/returning :id)
      (sql/format)))

(comment
  
  (select-snippet-by-id 4 (jt/instant))
  (select-recent-snippets 3 (jt/instant))
  (insert-snippet {:title "asdf" :content "wow" :created (jt/instant) :expires (jt/instant)})

  :rcf)
