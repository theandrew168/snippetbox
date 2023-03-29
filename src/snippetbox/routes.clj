(ns snippetbox.routes
    (:require [compojure.core :as c]
              [compojure.route :as route]
              [ring.middleware.params :as ring.params]
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
   (route/resources "/" {:root "public"})
   response/not-found))

(defn apply-middleware [routes]
  (-> routes
      ring.params/wrap-params
      middleware/wrap-secure-headers
      middleware/wrap-access-log
      middleware/wrap-errors))
