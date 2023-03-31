(ns snippetbox.response)

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

(defn not-found
  ([] (not-found nil))
  ([_] (response 404 "Not Found" {"Content-Type" "text/html; charset=utf8"})))

(defn unprocessable-content [body]
  (response 422 body {"Content-Type" "text/html; charset=utf8"}))

(defn internal-server-error []
  (response 500 "Internal Server Error" {"Content-Type" "text/html; charset=utf8"
                                         "Connection" "close"}))
