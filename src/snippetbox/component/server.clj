(ns snippetbox.component.server
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as httpkit]
            [snippetbox.routes :as routes]
            [snippetbox.store.postgresql :as postgresql]))

(defrecord Server [server database port threads]
  component/Lifecycle

  (start [this]
    (let [store (postgresql/store (:connection database))
          app (routes/apply-middleware (routes/routes store))
          server (httpkit/run-server app {:ip "127.0.0.1" :port port :thread threads})]
      (println (format "Listening on port %s..." port))
      (assoc this :server server)))

  (stop [this]
    (server)
    (assoc this :server nil)))
