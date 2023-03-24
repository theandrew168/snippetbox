(ns snippetbox.core
  (:require [compojure.core :as c]
            [compojure.route :as route]
            [org.httpkit.server :as httpd]
            [ring.middleware.params :as ring.params]
            [snippetbox.handler :as handler]
            [snippetbox.middleware :as middleware]
            [snippetbox.migrate :as migrate]
            [snippetbox.response :as response]
            [snippetbox.storage :as storage])
  (:gen-class))

;; TODO
;; SQL query builder (honeysql)
;; System management (integrant)

(def conf {:db "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"})

(defn routes [conn]
  (c/routes
   (c/GET "/" [] (partial handler/index conn))
   (c/GET "/snippet/view/:id" [] (partial handler/view conn))
   (c/GET "/snippet/create" [] (partial handler/create conn))
   (c/POST "/snippet/create" [] (partial handler/submit conn))
   (c/GET "/error" [] (fn [_] (response/internal-server-error)))
   (route/resources "/" {:root "public"})
   response/not-found))

(defn apply-middleware [routes]
  (-> routes
      ring.params/wrap-params
      middleware/wrap-secure-headers
      middleware/wrap-access-log
      middleware/wrap-errors))

(defn -main [& args]
  (let [n-cpu (.availableProcessors (Runtime/getRuntime))
        port (-> "PORT" System/getenv (or "5000") Integer/parseInt)
        conn (storage/connect (:db conf))
        app (apply-middleware (routes conn))]
    (println "Applying migrations...")
    (migrate/migrate! conn "migrations")
    (println (format "Listening on port %s..." port))
    (httpd/run-server app {:ip "127.0.0.1"
                           :port port
                           :thread (* 2 n-cpu)})))

(comment
  
  ;; connect to database
  (def conn (storage/connect (:db conf)))

  ;; apply migration(s)
  (migrate/migrate! conn "migrations")

  ;; init the application
  (def app (apply-middleware (routes conn)))

  ;; start the web server
  (def server (httpd/run-server app {:ip "127.0.0.1" :port 5000}))

  ;; stop the web server
  (server)

  :rcf)
