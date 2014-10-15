(ns hunter-api.handler
  (:use ring.util.response)
  (:require [compojure.core :refer [ANY
                                    DELETE
                                    GET
                                    HEAD
                                    OPTIONS
                                    POST
                                    PUT
                                    context
                                    defroutes]]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hunter-api.middleware :refer [wrap-exception-handler
                                        wrap-request-logger
                                        wrap-response-logger]]
            [hunter-api.http :as http]
            [hunter-api.data :as data]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [ring.middleware.json :refer [wrap-json-body]]))

(defroutes api-routes
  "Main client API route definitions"
  (context "/api" []
           (OPTIONS "/" []
                    (http/options [:options] {:version "0.1.0-SNAPSHOT"}))
           (ANY "/" []
                (http/method-not-allowed [:options]))
           (context "/datasets" []
                    (GET "/" []
                         (http/not-implemented))
                    (GET "/:id" [id]
                         (http/not-implemented))
                    (HEAD "/id" [id]
                          (http/not-implemented))
                    (POST "/" [:as req]
                          (http/not-implemented))
                    (PUT "/:id" [id]
                         (http/not-implemented))
                    (DELETE "/:id" [id]
                            (http/not-implemented))
                    (OPTIONS "/" []
                             (http/options [:options :get :head :put :post :delete]))
                    (ANY "/" []
                         (http/method-not-allowed [:options :get :head :put :post :delete]))))
  (route/not-found "Nothing to see here..."))

(def app
  "Application entry point and handler chain"
  (->
   (handler/api api-routes)
   (wrap-json-body)
   (wrap-request-logger)
   (wrap-exception-handler)
   (wrap-response-logger)
   (wrap-restful-response)))
