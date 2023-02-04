(ns snippetbox.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.page :as html]
            [org.httpkit.server :as httpd])
  (:gen-class))

(defn render-page [title main]
  (html/html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:title (format "%s - Snippetbox" title)]
    [:link {:rel "shortcut icon" :href "/img/favicon.ico" :type "image/x-icon"}]
    (html/include-css "/css/main.css")
    (html/include-css "https://fonts.googleapis.com/css?family=Ubuntu+Mono:400,700")]
   [:body
    [:header
     [:h1
      [:a {:href "/"} "Snippetbox"]]]
    [:nav
     [:a {:href "/"} "Home"]]
    main
    [:footer "Powered by "
     [:a {:href "https://clojure.org"} "Clojure"]]]))

(defn render-index []
  (render-page
   "Home"
   [:main
    [:h2 "Latest Snippets"]
    [:p "There's nothing to see here yet!"]]))

(defn html-response [code body]
  {:status code
   :headers {"Content-Type" "text/html; charset=utf8"}
   :body body})

(defn ok [body]
  (html-response 200 body))

(defn not-found [_]
  (html-response 404  "Not found"))

(defn index [_] 
  (ok (render-index)))

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
  (route/resources "/")
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