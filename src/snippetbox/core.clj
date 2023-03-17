(ns snippetbox.core
  (:require [compojure.core :refer [routes GET POST]]
            [compojure.route :as route]
            [hiccup.page :as html]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as dt]
            [org.httpkit.server :as httpd])
  (:gen-class))


(def conf {:db "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"})

;; database connection

(defn connect [db]
  (dt/read-as-instant)
  (jdbc/get-datasource db))

(defn migrate [conn]
  (jdbc/execute!
   conn
   ["CREATE TABLE IF NOT EXISTS snippet (
     id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
     title TEXT NOT NULL,
     content TEXT NOT NULL,
     created TIMESTAMPTZ NOT NULL,
     expires TIMESTAMPTZ NOT NULL
     )"]))

;; data access layer

(defn snippet-create [conn title content expires]
  (let [now (jt/instant)
        expires (jt/plus now (jt/days expires))]
    (jdbc/execute-one!
     conn
     ["INSERT INTO snippet (title, content, created, expires)
       VALUES (?, ?, ?, ?) RETURNING id" title content now expires])))

(defn snippet-list [conn at n]
  (jdbc/execute!
   conn
   ["SELECT * FROM snippet WHERE expires > ? ORDER BY created DESC LIMIT ?" at n]))

(defn snippet-read [conn id at]
  (jdbc/execute-one!
   conn
   ["SELECT * FROM snippet WHERE id = ? AND expires > ?" id at]))

(defn snippet-update [conn id title content]
  (jdbc/execute-one!
   conn
   ["UPDATE snippet SET title = ?, content = ? WHERE id = ?" title content id]))

(defn snippet-delete [conn id]
  (jdbc/execute-one!
   conn
   ["DELETE FROM snippet WHERE id = ?" id]))

;; HTML rendering

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

;; base response helpers

(defn html-response [code body]
  {:status code
   :headers {"Content-Type" "text/html; charset=utf8"}
   :body body})

(defn redirect-response [code body url]
  {:status code
   :headers {"Location" url}
   :body body})

;; response helpers

(defn ok [body]
  (html-response 200 body))

(defn created []
  (html-response 201 "Created"))

(defn found [url]
  (redirect-response 302 "Found" url))

(defn see-other [url]
  (redirect-response 303 "See Other" url))

(defn not-found [_]
  (html-response 404 "Not Found"))

;; handlers

(defn index [_ _]
  (ok (render-index)))

(defn view [conn req]
  (let [now (jt/instant)
        id (parse-long (-> req :params :id))
        snippet (snippet-read conn id now)]
    (if snippet
      (render-page "View" snippet)
      (not-found req))))

(defn create [_ _]
  (ok "Create snippet..."))

(defn submit [conn _]
  (let [res (snippet-create conn "O Snail" "O snail\nClimb Mount Fuji,\nBut slowly, slowly!\n\n– Kobayashi Issa" 7)
        id (:snippet/id res)
        url (format "/snippet/view/%d" id)]
    (see-other url)))

;; routes

(defn init-app [conf]
  (let [conn (connect (:db conf))]
    (routes
     (GET "/" [] (partial index conn))
     (GET "/snippet/view/:id" [] (partial view conn))
     (GET "/snippet/create" [] (partial create conn))
     (POST "/snippet/create" [] (partial submit conn))
     (route/resources "/")
     (route/not-found not-found))))


;; main

(defn -main [& args]
  (let [n-cpu (.availableProcessors (Runtime/getRuntime))
        port (-> "PORT" System/getenv (or "5000") Integer/parseInt)
        app (init-app conf)]
    (println (format "Listening on port %s..." port))
    (httpd/run-server app {:ip "127.0.0.1"
                           :port port
                           :thread (* 2 n-cpu)})))

(comment

  ;; start the web server
  (def server (httpd/run-server (init-app conf) {:ip "127.0.0.1" :port 5000}))

  ;; stop the web server
  (server)

  ;; connect to database
  (def conn (connect (:db conf)))

  ;; apply migration(s)
  (migrate conn)

  ;; undo migration(s)
  (jdbc/execute! conn ["DROP TABLE IF EXISTS snippet"])

  (snippet-create conn "Foo" "A tale about foo" 7)
  (snippet-list conn)
  (snippet-read conn 1 (jt/instant))
  (snippet-update conn 1 "Bar", "update the content")
  (snippet-delete conn 1)

  (render-page "Foo" "asdf")

  :rcf)
