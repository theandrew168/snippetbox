(ns snippetbox.handler
  (:require [java-time.api :as jt]
            [snippetbox.render :as render]
            [snippetbox.response :as response]
            [snippetbox.store.snippet :as snippet-store]
            [snippetbox.validate :as validate]))

(defn index [store _]
  (let [snippets (snippet-store/list-recent (:snippet store) 3 (jt/instant))]
    (response/ok (render/index snippets))))

(defn view-snippet [store req]
  (let [now (jt/instant)
        id (parse-long (-> req :params :id))
        snippet (snippet-store/read-by-id (:snippet store) id now)]
    (if snippet
      (response/ok (render/view-snippet snippet))
      (response/not-found req))))

(defn create-snippet [_ _]
  (let [form {:expires 365}]
    (response/ok (render/create-snippet form))))

(defn- form->snippet [{:keys [title content expires]}]
  (let [created (jt/instant)
        expires (jt/plus created (jt/days expires))]
    {:title title :content content :created created :expires expires}))

(defn create-snippet-form [store req]
  (let [params (:form-params req)
        form {:title (get params "title")
              :content (get params "content")
              :expires (parse-long (get params "expires"))}
        form (validate/snippet form)]
    (if (not-empty (:errors form))
      (response/unprocessable-content (render/create-snippet form))
      (let [snippet (form->snippet form)
            res (snippet-store/create (:snippet store) snippet)
            id (:snippet/id res)
            url (format "/snippet/view/%d" id)]
        (response/see-other url)))))
