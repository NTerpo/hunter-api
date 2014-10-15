(ns hunter-api.middleware
  (:require [cheshire.core :refer :all]
            [cheshire.generate :refer [add-encoder encode-str remove-encoder]]
            [clj-time.format :as format]
            [clojure.string :refer [upper-case]]
            [ring.util.response :refer [response status]]
            [taoensso.timbre :refer [debug warn]]
            [slingshot.slingshot :refer [try+]])
  (:import (com.fasterxml.jackson.core JsonGenerator)))

(add-encoder java.lang.Exception
             (fn [^Exception e ^JsonGenerator jg]
               (.writeStartObject jg)
               (.writeFieldName jg "exception")
               (.writeString jg (.getName (class e)))
               (.writeFieldName jg "message")
               (.writeString jg (.getMessage e))
               (.writeEndObject jg)))

(add-encoder org.joda.time.DateTime
             (fn [^org.joda.time.DateTime dt ^JsonGenerator jg]
               (.writeString jg (format/unparse
                                 (format/formatters :date-time-no-ms) dt))))

(add-encoder org.bson.types.ObjectId
             (fn [^org.bson.types.ObjectId id ^JsonGenerator jg]
               (.writeString jg (.toString id))))
;;
;; Middleware Handlers 
;;

(defn wrap-request-logger
  "Ring middleware function  that use clojure.tools.logging to write a debug message containing remote address, request method & URI of incoming request"
  [handler]
  (fn [req]
    (let [{remote-addr :remote-addr request-method :request-method uri :uri} req]
      (debug remote-addr (upper-case (name request-method)) uri)
      (handler req))))

(defn wrap-response-logger
  "Ring middleware function that uses clojure.tools.logging to write a debug message containing remote address, request method URI & response status of outgoing response"
  [handler]
  (fn [req]
    (let [response (handler req)
          {remote-addr :remote-addr request-method :request-method uri :uri} req
          {status :status body :body} response]
      (if (instance? Exception body)
        (warn body remote-addr (upper-case (name request-method)) uri "->" status body)
        (debug remote-addr (upper-case (name request-method)) uri "->" status))
      response)))

(defn wrap-exception-handler
  "Ring middleware function to trap any uncaught exceptions and return an appropriate status code with the exception instance as the response body"
  [handler]
  (fn [req]
    (try+
      (handler req)
      (catch [:type :doitnow.data/invalid] _
        (-> (response (&throw-context :message))
            (status 400)))
      (catch [:type :doitnow.data/not-found] _
        (-> (response (&throw-context :message))
            (status 404)))
      (catch Object _
        (-> (response (&throw-context :message))
            (status 500))))))
