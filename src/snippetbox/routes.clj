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
   (c/GET "/error" [] (fn [_] (response/internal-server-error)))
   (c/GET "/session-test" [] handler/session-test)
   (route/resources "/" {:root "public"})
   response/not-found))
