(ns snippetbox.handler
  (:require [java-time.api :as jt]
            [snippetbox.render :as render]
            [snippetbox.response :as response]
            [snippetbox.storage :as storage]
            [snippetbox.validate :as validate]))

(defn index [conn _]
  (let [snippets (storage/snippet-list conn (jt/instant) 3)]
    (response/ok (render/index snippets))))

(defn view [conn req]
  (let [now (jt/instant)
        id (parse-long (-> req :params :id))
        snippet (storage/snippet-read conn id now)]
    (if snippet
      (response/ok (render/view snippet))
      (response/not-found req))))

(defn create [_ _]
  (let [form {:expires 365}]
    (response/ok (render/create form))))

(defn submit [conn req]
  (let [params (:form-params req)
        form {:title (get params "title")
              :content (get params "content")
              :expires (parse-long (get params "expires"))}
        form (validate/snippet form)]
    (if (not-empty (:errors form))
      (response/unprocessable-content (render/create form))
      (let [res (storage/snippet-create conn form)
            id (:snippet/id res)
            url (format "/snippet/view/%d" id)]
        (response/see-other url)))))