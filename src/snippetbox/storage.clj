(ns snippetbox.storage
  (:require [integrant.core :as ig]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]
            [snippetbox.query :as query]))

(defmethod ig/init-key :app/database [_ {:keys [connection-string]}]
  (jdbc.date-time/read-as-instant)
  (jdbc/get-datasource connection-string))

(defn snippet-create [conn {:keys [expires] :as snippet}]
  (let [now (jt/instant)
        expires (jt/plus now (jt/days expires))
        snippet (merge snippet {:created now :expires expires})]
    (jdbc/execute-one! conn (query/insert-snippet snippet))))

(defn snippet-list [conn n at]
  (jdbc/execute! conn (query/select-recent-snippets n at)))

(defn snippet-read [conn id at]
  (jdbc/execute-one! conn (query/select-snippet-by-id id at)))

(comment

  (def system
    (-> "config.edn"
        slurp
        ig/read-string
        (ig/init [:app/database])))

  (ig/halt! system [:app/database])

  (snippet-create (:app/database system) {:title "Foo" :content "A tale about foo" :expires 7})
  (snippet-list (:app/database system) 3 (jt/instant))
  (snippet-read (:app/database system) 32 (jt/instant))

  :rcf)