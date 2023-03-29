(ns snippetbox.core
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]
            [org.httpkit.server :as httpkit]
            [snippetbox.routes :as routes]
            [snippetbox.storage.postgresql :as storage.postgresql])
  (:gen-class))

;; config - none (defaults in code + config.edn)
;; database - config
;; app - config, database

;; data types (models) have three "versions":
;; 1. The inbound type (no ID, possible conversions, usually created via form)
;; 2. The outbound type (no ID, possible conversions, usually rendered via html)
;; 3. The domain type (all data, raw, used by the app for internal processing)

(defrecord Database [connection uri]
  component/Lifecycle

  (start [this]
    (jdbc.date-time/read-as-instant)
    (let [conn (jdbc/get-datasource uri)]
      (assoc this :connection conn)))

  (stop [this]
    (assoc this :connection nil)))

(defrecord WebServer [server database port threads]
  component/Lifecycle

  (start [this]
    (let [storage (storage.postgresql/init (:connection database))
          app (routes/apply-middleware (routes/routes storage))
          server (httpkit/run-server app {:ip "127.0.0.1" :port port :thread threads})]
      (println (format "Listening on port %s..." port))
      (assoc this :server server)))

  (stop [this]
    (server)
    (assoc this :server nil)))

(defn system [config]
  (let [{:keys [uri port threads]} config]
    (component/system-map
     :database (map->Database {:uri uri})
     :webserver (component/using
                 (map->WebServer {:port port :threads threads})
                 [:database]))))

(defn default-config []
  {:uri "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"
   :port 5000
   :threads (.availableProcessors (Runtime/getRuntime))})

(defn -main [& args]
  (let [config (default-config)]
    (component/start (system config))))

(comment

  (def sys (system (default-config)))
  (alter-var-root #'sys component/start)
  (alter-var-root #'sys component/stop)

  :rcf)