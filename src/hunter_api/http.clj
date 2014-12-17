(ns hunter-api.http
  (:require [clojure.string :refer [join upper-case]]
            [ring.util.response :refer [header response status]]))

(defn url-from
  "Create a location URL from request data and additional path elements"
  [{scheme :scheme server-name :server-name server-port :server-port uri :uri}
   & path-elements]
  (str "http://" server-name ":" server-port uri "/" (join "/" path-elements)))

(defn options
  "Generates a 200 HTTP response with an Allow header containing the provided HTTP method names - response for an HTTP OPTIONS request"
  ([] (options #{:options} nil))
  ([allowed] (options allowed nil))
  ([allowed body]
     (-> (response body)
         (header "Allow" (join ", " (map (comp upper-case name) allowed))))))

(defn ok
  "Returns an HTTP 200 (OK)"
  [body]
  (-> (response body)
      (status 200)))

(defn created
  "Returns an HTTP 201 (Created)"
  ([url]
     (created url nil))
  ([url body]
     (-> (response body)
         (status 201)
         (header "location" url))))

(defn no-content?
  "Checks for a nil or empty response and set status to 204 (No Content) with nil body"
  [body]
  (if (or (nil? body) (empty? body))
    (-> (response nil)
        (status 204))
    (response body)))

(defn no-content
  "Returns an HTTP 204 (No Content)"
  []
  (-> (response nil)
      (status 204)))

(defn bad-request
  "Returns an HTTP 400 (Bad Request)"
  []
  (-> (response nil)
      (status 400)))

(defn method-not-allowed
  "Generates a 405 response with an Allow header containing the provided HTTP method names"
  [allowed]
  (-> (options allowed)
      (status 405)))

(defn unprocessable-entity
  "Returns an HTTP 422 (Unprocessable Entity)"
  []
  (-> (response nil)
      (status 422)))

(defn not-implemented
  "Returns an HTTP 501 (Not Implemented)"
  []
  (-> (response nil)
      (status 501)))
