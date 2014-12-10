(ns hunter-api.handler
  (:use ring.util.response)
  (:import (org.eclipse.jetty.util.thread QueuedThreadPool))
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
            [hunter-api.util :as util]
            [cheshire.core :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]))

(defroutes api-routes
  "Main client API route definitions"
  (context "/api" []
           (OPTIONS "/" []
                    (http/options [:options] {:version "0.2.0"}))
           (ANY "/" []
                (http/method-not-allowed [:options]))
           (context "/datasets" []
                    (GET "/" [:as req]
                         (if (nil? (req :query-string))
                           (http/not-implemented)
                           (http/ok (data/find-dataset
                                     (util/query-string->hashmap
                                      (req :query-string))))))
                    (GET "/:id" [id]
                         (http/ok (data/get-dataset id)))
                    (HEAD "/id" [id]
                          (http/not-implemented))
                    (POST "/" request 
                          (let [ds (data/create-dataset (request :body))
                                location (http/url-from request (str (ds :_id)))]
                            (http/created location ds)))
                    (PUT "/" request
                         ;; (data/update-dataset ((request :body) :args) ((request :body) :new-args))
                         (http/not-implemented))
                    (DELETE "/:id" [id]
                            ;; (http/ok (data/delete-dataset id))
                            (http/not-implemented))
                    (OPTIONS "/" []
                             (http/options [:options :get :head :put :post :delete]))
                    (ANY "/" []
                         (http/method-not-allowed [:options :get :head :put :post :delete])))
           (context "/healthcheck" []
                    (GET "/" []
                         (http/ok {}))
                    (ANY "/" []
                         (http/method-not-allowed [:options :get :head :put :post :delete]))))
  (route/not-found "Nothing to hunt here..."))

(def app
  "Application entry point and handler chain"
  (->
   (handler/api api-routes)
   (wrap-json-body {:keywords? true})
   (wrap-request-logger)
   (wrap-exception-handler)
   (wrap-response-logger)
   (wrap-json-response)
   (wrap-restful-response)))

(defn -main [& args]
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    (run-jetty app
               {:port port
                :configurator #(.setThreadPool % (QueuedThreadPool. 5))})))
