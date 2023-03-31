(ns snippetbox.routes
  (:require [clojure.string]
            [compojure.core :as c]
            [compojure.route :as route]
            [snippetbox.handler :as handler]
            [snippetbox.response :as response]))

(defn routes [store]
  (c/routes
   (c/GET "/" [] (partial handler/index store))
   (c/GET "/snippet/view/:id" [] (partial handler/view-snippet store))
   (c/GET "/snippet/create" [] (partial handler/create-snippet store))
   (c/POST "/snippet/create" [] (partial handler/create-snippet-form store))
   (c/GET "/user/register" [] (partial handler/register store))
   (c/POST "/user/register" [] (partial handler/register-form store))
   (c/GET "/user/login" [] (partial handler/login store))
   (c/POST "/user/login" [] (partial handler/login-form store))
   (c/POST "/user/logout" [] (partial handler/logout-form store))
   (route/resources "/" {:root "public"})
   response/not-found))
