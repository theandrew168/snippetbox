(ns snippetbox.routes
    (:require [compojure.core :as c]
              [compojure.route :as route]
              [ring.middleware.params :as ring.params]
              [snippetbox.handler :as handler]
              [snippetbox.middleware :as middleware]
              [snippetbox.response :as response]))

(defn routes [conn]
  (c/routes
   (c/GET "/" [] (partial handler/index conn))
   (c/GET "/snippet/view/:id" [] (partial handler/view-snippet conn))
   (c/GET "/snippet/create" [] (partial handler/create-snippet conn))
   (c/POST "/snippet/create" [] (partial handler/create-snippet-form conn))
   (c/GET "/error" [] (fn [_] (response/internal-server-error)))
   (route/resources "/" {:root "public"})
   response/not-found))

(defn apply-middleware [routes]
  (-> routes
      ring.params/wrap-params
      middleware/wrap-secure-headers
      middleware/wrap-access-log
      middleware/wrap-errors))
