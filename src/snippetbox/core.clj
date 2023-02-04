(ns snippetbox.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [org.httpkit.server :as httpd])
  (:gen-class))

(defn html-response [code body]
  {:status code
   :headers {"Content-Type" "text/html; charset=utf8"}
   :body body})

(defn ok [body]
  (html-response 200 body))

(defn not-found [_]
  (html-response 404  "Not found"))

(defn index [_]
  (ok "Hello from Snippetbox"))

(defn view [req]
  (if-let [id (parse-long (-> req :params :id))]
    (str "id = " id)
    (not-found req)))

(defn create [_]
  (ok "Create a new snippet form..."))

(defn submit [_]
  (ok "Submit a new snippet..."))

(defroutes app
  (GET "/" [] index)
  (GET "/snippet/view/:id" [] view)
  (GET "/snippet/create" [] create)
  (POST "/snippet/create" [] submit)
  (route/not-found not-found))

(defn -main [& args]
  (println "Listening on port 5000...")
  (httpd/run-server app {:port 5000}))

(comment

  ;; start the web server
  (def server (httpd/run-server #'app {:port 5000}))

  ;; stop the web server
  (server)

  :rcf)