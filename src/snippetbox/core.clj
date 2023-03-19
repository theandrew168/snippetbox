(ns snippetbox.core
  (:require [clojure.string :as s]
            [compojure.core :as c]
            [compojure.route :as route]
            [hiccup.page :as html]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as dt]
            [org.httpkit.server :as httpd]
            [ring.util.request :as ring.request]
            [ring.util.request :as req])
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

;; HTML rendering helpers

(defn current-year []
  (jt/year))

(defn human-date [instant]
  (let [utc (jt/zoned-date-time instant "UTC")
        date (jt/format "dd MMM YYYY" utc)
        time (jt/format "kk:mm" utc)]
    (str date " at " time)))

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
    [:footer "Powered by " [:a {:href "https://clojure.org"} "Clojure"] " in " (current-year)]]))

(defn render-index [snippets]
  (render-page
   "Home"
   [:main
    [:h2 "Latest Snippets"]
    (if (not-empty snippets)
      [:table
       [:tr
        [:th "Title"]
        [:th "Created"]
        [:th "ID"]]
       (for [snippet snippets]
         [:tr
          [:td
           [:a {:href (format "/snippet/view/%d" (:snippet/id snippet))} (:snippet/title snippet)]]
          [:td (human-date (:snippet/created snippet))]
          [:td "#" (:snippet/id snippet)]])]
      [:p "There's nothing to see here... yet!"])]))

(defn render-view [snippet]
  (render-page
   (format "Snippet #%d" (:snippet/id snippet))
   [:main
    [:div {:class "snippet"}
     [:div {:class "metadata"}
      [:strong (:snippet/title snippet)]
      [:span "#" (:snippet/id snippet)]]
     [:pre
      [:code (:snippet/content snippet)]]
     [:div {:class "metadata"}
      [:time "Created: " (human-date (:snippet/created snippet))]
      [:time "Expires: " (human-date (:snippet/expires snippet))]]]]))

;; base response helpers

(defn response [code headers body]
  {:status code
   :headers headers
   :body body})

(defn html-response [code body]
  (response code {"Content-Type" "text/html; charset=utf8"} body))

(defn redirect-response [code body url]
  (response code {"Location" url} body))

;; response helpers

(defn ok [body]
  (html-response 200 body))

(defn see-other [url]
  (redirect-response 303 "See Other" url))

(defn not-found [_]
  (html-response 404 "Not Found"))

(defn internal-server-error []
  (html-response 500 "Internal Server Error"))

;; handlers

(defn index [conn _]
  (let [snippets (snippet-list conn (jt/instant) 3)]
    (ok (render-index snippets))))

(defn view [conn req]
  (let [now (jt/instant)
        id (parse-long (-> req :params :id))
        snippet (snippet-read conn id now)]
    (if snippet
      (ok (render-view snippet))
      (not-found req))))

(defn create [_ _]
  (ok "Create snippet..."))

(defn submit [conn _]
  (let [res (snippet-create conn "O Snail" "O snail\nClimb Mount Fuji,\nBut slowly, slowly!\n\n– Kobayashi Issa" 7)
        id (:snippet/id res)
        url (format "/snippet/view/%d" id)]
    (see-other url)))

;; middleware

(defn add-headers [r headers]
  (reduce (fn [r k] (assoc-in r [:headers k] (get headers k)))
          r
          (keys headers)))

(defn wrap-secure-headers [handler]
    (let [headers {"Referrer-Policy" "origin-when-cross-origin"
                   "X-Content-Type-Options" "nosniff"
                   "X-Frame-Options" "deny"
                   "X-XSS-Protection" "0"}]
      (fn [req]
        (let [resp (handler req)]
          (add-headers resp headers)))))

(defn wrap-access-log [handler]
  (fn [req]
    (let [addr (:remote-addr req)
          method (s/upper-case (name (:request-method req)))
          url (ring.request/request-url req)]
      (printf "%s - %s %s\n" addr method url)
      (flush)
      (handler req))))

(defn wrap-errors [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (do
             (.printStackTrace e)
             (internal-server-error))))))

;; routes

(defn init-routes [conf]
  (let [conn (connect (:db conf))]
    (c/routes
     (c/GET "/" [] (partial index conn))
     (c/GET "/snippet/view/:id" [] (partial view conn))
     (c/GET "/snippet/create" [] (partial create conn))
     (c/POST "/snippet/create" [] (partial submit conn))
     (route/resources "/")
     (route/not-found not-found))))

(defn init-app [conf]
  (-> (init-routes conf)
      (c/wrap-routes wrap-secure-headers)
      (c/wrap-routes wrap-access-log)
      (c/wrap-routes wrap-errors)))

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
  (snippet-list conn (jt/instant) 3)
  (snippet-read conn 1 (jt/instant))
  (snippet-update conn 1 "Bar", "update the content")
  (snippet-delete conn 1)

  :rcf)
