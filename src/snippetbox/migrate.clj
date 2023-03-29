(ns snippetbox.migrate
  (:require [clojure.set :refer [difference]]
            [clojure.string :refer [split]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [resauce.core :as resauce]))

(defn- create-migration-table! [conn]
  (jdbc/execute-one!
   conn
   ["CREATE TABLE IF NOT EXISTS migration (
     id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
     name TEXT NOT NULL UNIQUE
   )"]))

(defn- applied-migrations [conn]
  (jdbc/execute!
   conn
   ["SELECT name FROM migration"]
   {:builder-fn rs/as-unqualified-maps}))

(defn- extract-name [url]
  (-> url
      .getPath
      (split #"/")
      last))

(defn- desired-migrations [dir]
  (map #(hash-map :name (extract-name %) :url %)
       (resauce/resource-dir dir)))

(defn- pending-migrations [desired applied]
  (let [desired-keys (set (map :name desired))
        applied-keys (set (map :name applied))
        pending-keys (difference desired-keys applied-keys)]
    (filter #(pending-keys (:name %)) desired)))

(defn- apply-migration! [conn {:keys [name url]}]
  (jdbc/with-transaction [tx conn]
    (jdbc/execute-one! tx [(slurp url)])
    (jdbc/execute-one! tx ["INSERT INTO migration (name) VALUES (?)" name])))

(defn migrate! [conn]
  (create-migration-table! conn)
  (let [desired (desired-migrations "migrations")
        applied (applied-migrations conn)
        pending (pending-migrations desired applied)]
    (doall
     (for [migration (sort-by :name pending)]
       (apply-migration! conn migration)))))

(comment

  (applied-migrations "jdbc:postgresql://postgres:postgres@localhost:5432/postgres")

  (desired-migrations "migrations")
  (sort-by :name (desired-migrations "migrations"))

  (pending-migrations
   (desired-migrations "migrations")
   (applied-migrations "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"))
  
  (migrate!
   "jdbc:postgresql://postgres:postgres@localhost:5432/postgres")

  :rcf)
