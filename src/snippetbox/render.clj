(ns snippetbox.render
  (:require [hiccup.page :as html]
            [java-time.api :as jt]))

(defn current-year []
  (jt/year))

(defn human-date [instant]
  (let [utc (jt/zoned-date-time instant "UTC")
        date (jt/format "dd MMM YYYY" utc)
        time (jt/format "kk:mm" utc)]
    (str date " at " time)))

(defn- page [title main]
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
     [:div
      [:a {:href "/"} "Home"]
      [:a {:href "/snippet/create"} "Create snippet"]]
     [:div
      [:a {:href "/user/register"} "Register"]
      [:a {:href "/user/login"} "Login"]
      [:form {:method "POST" :action "/user/logout"}
       [:button "Logout"]]]]
    main
    [:footer "Powered by " [:a {:href "https://clojure.org"} "Clojure"] " in " (current-year)]]))

(defn index [snippets]
  (page
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

(defn view-snippet [snippet flash]
  (page
   (format "Snippet #%d" (:snippet/id snippet))
   [:main
    (when flash
      [:div {:class "flash"} flash])
    [:div {:class "snippet"}
     [:div {:class "metadata"}
      [:strong (:snippet/title snippet)]
      [:span "#" (:snippet/id snippet)]]
     [:pre
      [:code (:snippet/content snippet)]]
     [:div {:class "metadata"}
      [:time "Created: " (human-date (:snippet/created snippet))]
      [:time "Expires: " (human-date (:snippet/expires snippet))]]]]))

(defn create-snippet [{:keys [title content expires errors]}]
  (page
   "Create a New Snippet"
   [:main
    [:form {:method "POST" :action "/snippet/create"}
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

(defn register [{:keys [name email errors]}]
  (page
   "Register"
   [:main
    [:form {:method "POST" :action "/user/register" :novalidate true}
     [:div
      [:label "Name:"]
      (when-let [error (:name errors)]
        [:label {:class "error"} error])
      [:input {:type "text" :name "name" :value name}]]
     [:div
      [:label "Email:"]
      (when-let [error (:email errors)]
        [:label {:class "error"} error])
      [:input {:type "email" :name "email" :value email}]]
     [:div
      [:label "Password:"]
      (when-let [error (:password errors)]
        [:label {:class "error"} error])
      [:input {:type "password" :name "password"}]]
     [:div
      [:input {:type "submit" :value "Register"}]]]]))

(defn login
  ([form] (login form nil))
  ([{:keys [email errors]} flash]
   (page
    "Login"
    [:main
     (when flash
       [:div {:class "flash"} flash])
     [:form {:method "POST" :action "/user/login" :novalidate true}
      (for [error (:errors errors)]
        [:div {:class "error"} error])
      [:div
       [:label "Email:"]
       (when-let [error (:email errors)]
         [:label {:class "error"} error])
       [:input {:type "email" :name "email" :value email}]]
      [:div
       [:label "Password:"]
       (when-let [error (:password errors)]
         [:label {:class "error"} error])
       [:input {:type "password" :name "password"}]]
      [:div
       [:input {:type "submit" :value "Login"}]]]])))
