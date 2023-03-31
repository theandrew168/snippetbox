(ns snippetbox.routes
  (:require [clojure.string]
            [compojure.core :as c]
            [compojure.route :as route]
            [ring.middleware.params :as ring.params]
            [ring.middleware.session :as ring.session]
            [ring.middleware.session.cookie :as ring.cookie]
            [snippetbox.handler :as handler]
            [snippetbox.middleware :as middleware]
            [snippetbox.response :as response]))

(defn routes [store]
  (c/routes
   (c/GET "/" [] (partial handler/index store))
   (c/GET "/snippet/view/:id" [] (partial handler/view-snippet store))
   (c/GET "/snippet/create" [] (partial handler/create-snippet store))
   (c/POST "/snippet/create" [] (partial handler/create-snippet-form store))
   (c/GET "/error" [] (fn [_] (response/internal-server-error)))
   (c/GET "/session-test" [] handler/session-test)
   (route/resources "/" {:root "public"})
   response/not-found))

(defn hex->bytes [s]
  (map #(Integer/parseUnsignedInt (apply str %) 16) (partition 2 s)))

(defn apply-middleware [routes secret-key]
  (let [key (byte-array (hex->bytes secret-key))]
    (-> routes
        ring.params/wrap-params
        (ring.session/wrap-session {:store (ring.cookie/cookie-store {:key key})})
        middleware/wrap-secure-headers
        middleware/wrap-access-log
        middleware/wrap-errors)))
