(ns snippetbox.core
  (:require [clojure.repl :refer [pst]]
            [clojure.string :refer [upper-case]]
            [compojure.core :as c]
            [compojure.route :as route]
            [hiccup.page :as html]
            [java-time.api :as jt]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time :as jdbc.date-time]
            [org.httpkit.server :as httpd]
            [ring.middleware.params :as ring.params]
            [ring.util.request :as ring.request]
            [snippetbox.migrate :as migrate])
  (:gen-class))

;; TODO
;; SQL query builder (honeysql)
;; System management (integrant)

(def conf {:db "jdbc:postgresql://postgres:postgres@localhost:5432/postgres"})

;; data access layer

(defn snippet-create [conn {:keys [title content expires]}]
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
     [:a {:href "/"} "Home"]
     [:a {:href "/snippet/create"} "Create snippet"]]
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

(defn render-create [{:keys [title content expires errors]}]
  (render-page
   "Create a New Snippet"
   [:main
    [:form {:action "/snippet/create" :method "POST"}
     [:div
      [:label "Title"]
      (when-let [error (:title errors)]
        [:label {:class "error"} error])
      [:input {:type "text" :name "title" :value title}]]
     [:div
      [:label "Content"]
      (when-let [error (:content errors)]
        [:label {:class "error"} error])
      [:textarea {:name "content"} content]]
     [:div
      [:label "Delete in:"]
      (when-let [error (:expires errors)]
        [:label {:class "error"} error])
      [:input (merge {:type "radio" :name "expires" :value "365"} (when (= 365 expires) {:checked true})) " One Year"]
      [:input (merge {:type "radio" :name "expires" :value "7"} (when (= 7 expires) {:checked true})) " One Week"]
      [:input (merge {:type "radio" :name "expires" :value "1"} (when (= 1 expires) {:checked true})) " One Day"]]
     [:div
      [:input {:type "submit" :value "Publish snippet"}]]]]))

;; response helpers

(defn response
  ([status body]
   (response status body {}))
  ([status body headers]
   {:status status
    :body body
    :headers headers}))

(defn ok [body]
  (response 200 body {"Content-Type" "text/html; charset=utf8"}))

(defn see-other [url]
  (response 303 "See Other" {"Location" url}))

(defn not-found [_]
  (response 404 "Not Found" {"Content-Type" "text/html; charset=utf8"}))

(defn unprocessable-content [body]
  (response 422 body {"Content-Type" "text/html; charset=utf8"}))

(defn internal-server-error []
  (response 500 "Internal Server Error" {"Content-Type" "text/html; charset=utf8"
                                         "Connection" "close"}))

;; input validation

(defn validate [m key pred message]
  (if (pred (get m key))
    m
    (assoc-in m [:errors key] message)))

(defn validate-snippet [snippet]
  (-> snippet
      (validate :title not-empty "This field cannot be blank")
      (validate :title #(<= (count %) 100) "This field cannot be more than 100 characters long")
      (validate :content not-empty "This field cannot be blank")
      (validate :expires #{1 7 365} "This field must equal 1, 7, or 365")))

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
  (let [form {:expires 365}]
    (ok (render-create form))))

(defn submit [conn req]
  (let [params (:form-params req)
        form {:title (get params "title")
              :content (get params "content")
              :expires (parse-long (get params "expires"))}
        form (validate-snippet form)]
    (if (not-empty (:errors form))
      (unprocessable-content (render-create form))
      (let [res (snippet-create conn form)
            id (:snippet/id res)
            url (format "/snippet/view/%d" id)]
        (see-other url)))))

;; middleware

(defn wrap-secure-headers [handler]
  (let [headers {"Referrer-Policy" "origin-when-cross-origin"
                 "X-Content-Type-Options" "nosniff"
                 "X-Frame-Options" "deny"
                 "X-XSS-Protection" "0"}]
    (fn [req]
      (let [resp (handler req)]
        (assoc-in resp [:headers] (merge headers (:headers resp)))))))

(defn wrap-access-log [handler]
  (fn [req]
    (let [addr (:remote-addr req)
          method (upper-case (name (:request-method req)))
          url (ring.request/request-url req)]
      (printf "%s - %s %s\n" addr method url)
      (flush)
      (handler req))))

(defn wrap-errors [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           (do
             (pst e)
             (internal-server-error))))))

;; routes

; database connection depends on config
(defn connect-db [db]
  (jdbc.date-time/read-as-instant)
  (jdbc/get-datasource db))

; routes depend on database connection
(defn init-routes [conn]
  (c/routes
   (c/GET "/" [] (partial index conn))
   (c/GET "/snippet/view/:id" [] (partial view conn))
   (c/GET "/snippet/create" [] (partial create conn))
   (c/POST "/snippet/create" [] (partial submit conn))
   (c/GET "/error" [] (fn [_] (internal-server-error)))
   (route/resources "/" {:root "public"})
   not-found))

(defn apply-middleware [routes]
  (-> routes
      ring.params/wrap-params
      wrap-secure-headers
      wrap-access-log
      wrap-errors))

;; main

(defn -main [& args]
  (let [n-cpu (.availableProcessors (Runtime/getRuntime))
        port (-> "PORT" System/getenv (or "5000") Integer/parseInt)
        conn (connect-db (:db conf))
        app (apply-middleware (init-routes conn))]
    (println "Applying migrations...")
    (migrate/migrate! conn "migrations")
    (println (format "Listening on port %s..." port))
    (httpd/run-server app {:ip "127.0.0.1"
                           :port port
                           :thread (* 2 n-cpu)})))

(comment
  
  ;; connect to database
  (def conn (connect-db (:db conf)))

  ;; init the application
  (def app (apply-middleware (init-routes conn)))

  ;; start the web server
  (def server (httpd/run-server app {:ip "127.0.0.1" :port 5000}))

  ;; stop the web server
  (server)

  ;; apply migration(s)
  (migrate/migrate! conn "migrations")

  (snippet-create conn {:title "Foo" :content "A tale about foo" :expires 7})
  (snippet-list conn (jt/instant) 3)
  (snippet-read conn 1 (jt/instant))
  (snippet-update conn 1 "Bar", "update the content")
  (snippet-delete conn 1)

  (validate {:title ""} :title not-empty "must not be empty")
  
  :rcf)
