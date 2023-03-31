(ns snippetbox.handler
  (:require [java-time.api :as jt]
            [snippetbox.render :as render]
            [snippetbox.response :as response]
            [snippetbox.storage :as storage]
            [snippetbox.validate :as validate]))

(defn index [store _]
  (let [snippets (storage/list-recent-snippets store 3 (jt/instant))]
    (response/ok (render/index snippets))))

(defn view-snippet [store req]
  (let [session (:session req)
        flash (:flash session)
        now (jt/instant)
        id (parse-long (-> req :params :id))
        snippet (storage/read-snippet-by-id store id now)]
    (if snippet
      (let [session (assoc session :flash nil)]
        (-> (render/view-snippet snippet flash)
            response/ok
            (assoc :session session)))
      (response/not-found req))))

(defn create-snippet [_ _]
  (let [form {:expires 365}]
    (response/ok (render/create-snippet form))))

(defn- form->snippet [{:keys [title content expires]}]
  (let [created (jt/instant)
        expires (jt/plus created (jt/days expires))]
    {:title title :content content :created created :expires expires}))

(defn create-snippet-form [store req]
  (let [session (:session req)
        params (:form-params req)
        form {:title (get params "title")
              :content (get params "content")
              :expires (parse-long (get params "expires"))}
        form (validate/snippet form)]
    (if (not-empty (:errors form))
      (response/unprocessable-content (render/create-snippet form))
      (let [snippet (form->snippet form)
            res (storage/create-snippet store snippet)
            id (:snippet/id res)
            url (format "/snippet/view/%d" id)
            session (assoc session :flash "Snippet successfully created!")]
        (-> (response/see-other url)
            (assoc :session session))))))

(defn register [store req]
  (response/ok "TODO"))

(defn register-form [store req]
  (response/ok "TODO"))

(defn login [store req]
  (response/ok "TODO"))

(defn login-form [store req]
  (response/ok "TODO"))

(defn logout-form [store req]
  (response/ok "TODO"))
