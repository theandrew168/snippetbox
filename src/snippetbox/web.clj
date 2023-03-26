(ns snippetbox.web
  (:require [integrant.core :as ig]
            [org.httpkit.server :as httpd]
            [snippetbox.routes :as routes]))

;; TODO set default threads to 2xCPU but allow override via conf
(defmethod ig/init-key :app/web [_ {:keys [port database]}]
  (println (format "Listening on port %s..." port))
  (-> database
      routes/routes
      routes/apply-middleware
      (httpd/run-server {:ip "127.0.0.1"
                         :port port
                         :thread (* 2 (.availableProcessors (Runtime/getRuntime)))})))

(defmethod ig/halt-key! :app/web [_ server]
  (server))