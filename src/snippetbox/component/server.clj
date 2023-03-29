(ns snippetbox.component.server
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as httpkit]
            [snippetbox.routes :as routes]
            [snippetbox.storage.postgresql :as storage.postgresql]))

(defrecord Server [server database port threads]
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
