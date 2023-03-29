(ns snippetbox.component.database
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]))

(defrecord Database [connection uri]
  component/Lifecycle

  (start [this]
    (jdbc.date-time/read-as-instant)
    (let [conn (jdbc/get-datasource uri)]
      (assoc this :connection conn)))

  (stop [this]
    (assoc this :connection nil)))
