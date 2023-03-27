(ns snippetbox.core
  (:require [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]
            [org.httpkit.server :as httpd]
            [snippetbox.routes :as routes]
            [snippetbox.storage.snippet :as storage.snippet])
  (:gen-class))

;; config - none (defaults in code + config.edn)
;; database - config
;; app - config, database

;; data types (models) have three "versions":
;; 1. The inbound type (no ID, possible conversions, usually created via form)
;; 2. The outbound type (no ID, possible conversions, usually rendered via html)
;; 3. The domain type (all data, raw, used by the app for internal processing)

(defmethod ig/init-key :app/storage [_ {:keys [connection-string]}]
  (jdbc.date-time/read-as-instant)
  (-> (jdbc/get-datasource connection-string)
      storage.snippet/->PostgreSQLSnippetStorage))

;; TODO set default threads to 2xCPU but allow override via conf
(defmethod ig/init-key :app/web [_ {:keys [port storage]}]
  (println (format "Listening on port %s..." port))
  (let [app (routes/apply-middleware (routes/routes storage))
        n-cpu (.availableProcessors (Runtime/getRuntime))]
    (httpd/run-server app {:ip "127.0.0.1"
                           :port port
                           :thread n-cpu})))

(defmethod ig/halt-key! :app/web [_ server]
  (server))

(defn -main [& args]
  (-> "config.edn"
      slurp
      ig/read-string
      ig/init))

(comment

  (def system
    (-> "config.edn"
        slurp
        ig/read-string
        ig/init))
  
  (ig/halt! system)

  :rcf)
