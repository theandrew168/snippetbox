(ns snippetbox.core
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [hiccup.page :as html]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as dt]
            [org.httpkit.server :as httpd])
  (:gen-class))

(def db "jdbc:sqlite:snippetbox.db")

(defn migrate [db]
  (jdbc/execute!
   db
   ["CREATE TABLE IF NOT EXISTS snippet (
     id INTEGER NOT NULL PRIMARY KEY,
     title TEXT NOT NULL,
     content TEXT NOT NULL,
     created DATETIME NOT NULL,
     expires DATETIME NOT NULL
     )"]))

(defn snippet-create [db title content expires]
  (let [created (jt/local-date-time)
        expires (jt/plus created (jt/days expires))]
    (print created)
    (jdbc/execute!
     db
     ["INSERT INTO snippet (title, content, created, expires)
       VALUES (?, ?, ?, ?)" title content created expires])))

(defn snippet-list [db]
  (jdbc/execute!
   db
   ["SELECT * FROM snippet"]))

(defn snippet-read [db id]
  (jdbc/execute-one!
   db
   ["SELECT * FROM snippet WHERE id = ?" id]))

(defn snippet-update [db id title content]
  (jdbc/execute!
   db
   ["UPDATE snippet SET title = ?, content = ? WHERE id = ?" title content id]))

(defn snippet-delete [db id]
  (jdbc/execute!
   db
   ["DELETE FROM snippet WHERE id = ?" id]))

(defn render-page [title main]
  (html/html5
   {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:title (format "%s - Snippetbox" title)]
    [:link {:href "/img/favicon.ico" :rel "icon"}]
    [:link {:href "/css/main.css" :rel "stylesheet"}]
    [:link {:href "https://fonts.googleapis.com/css?family=Ubuntu+Mono:400,700" :rel "stylesheet"}]
    [:script {:src "/js/main.js" :defer true}]]
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
  (let [n-cpu (.availableProcessors (Runtime/getRuntime))
        port (-> "PORT" System/getenv (or "5000") Integer/parseInt)]
    (println (format "Listening on port %s..." port))
    (httpd/run-server app {:ip "127.0.0.1"
                           :port port
                           :thread (* 2 n-cpu)})))

(comment

  ;; start the web server
  (def server (httpd/run-server #'app {:ip "127.0.0.1" :port 5000}))

  ;; stop the web server
  (server)

  ;; apply migration(s)
  (migrate db)

  ;; undo migration(s)
  (jdbc/execute! db ["DROP TABLE IF EXISTS snippet"])

  (snippet-create db "Foo" "A tale about foo" 7)
  (snippet-list db)
  (snippet-read db 1)
  (snippet-update db 1 "Bar", "update the content")
  (snippet-delete db 1)

  (type (:snippet/created (first (snippet-list db))))

  (render-page "Foo" "asdf")

  :rcf)