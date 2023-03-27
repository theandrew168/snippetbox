(ns snippetbox.handler
  (:require [java-time.api :as jt]
            [snippetbox.render :as render]
            [snippetbox.response :as response]
            [snippetbox.storage :as storage]
            [snippetbox.validate :as validate]))

(defn index [conn _]
  (let [snippets (storage/snippet-list conn 3 (jt/instant))]
    (response/ok (render/index snippets))))

(defn view-snippet [conn req]
  (let [now (jt/instant)
        id (parse-long (-> req :params :id))
        snippet (storage/snippet-read conn id now)]
    (if snippet
      (response/ok (render/view-snippet snippet))
      (response/not-found req))))

(defn create-snippet [_ _]
  (let [form {:expires 365}]
    (response/ok (render/create-snippet form))))

(defn form->snippet [{:keys [title content expires]}]
  (let [created (jt/instant)
        expires (jt/plus created (jt/days expires))]
    {:title title :content content :created created :expires expires}))

(defn create-snippet-form [conn req]
  (let [params (:form-params req)
        form {:title (get params "title")
              :content (get params "content")
              :expires (parse-long (get params "expires"))}
        form (validate/snippet form)]
    (if (not-empty (:errors form))
      (response/unprocessable-content (render/create-snippet form))
      (let [snippet (form->snippet form)
            res (storage/snippet-create conn snippet)
            id (:snippet/id res)
            url (format "/snippet/view/%d" id)]
        (response/see-other url)))))