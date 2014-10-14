(ns hunter-api.handler
  (:use ring.util.response)
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.format-response :refer [wrap-restful-response]]))

(defroutes api-routes
  "Main client API route definitions"
  (context "/api" []
           (OPTIONS "/" []
                    (-> (response {:version "0.1.0-SNAPSHOT"})
                        (header "Allow" "OPTIONS"))))
  (route/not-found "Not Found"))

(def app
  (->
   (handler/api api-routes)
   (wrap-restful-response)))
