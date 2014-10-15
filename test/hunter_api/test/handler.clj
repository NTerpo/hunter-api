(ns hunter-api.test.handler
  (:require [clojure.test :refer :all]
            [hunter-api.handler :refer :all]
            [ring.mock.request :as mock]))

(deftest test-api-routes
  (testing "API options"
    (let [response (api-routes (mock/request :options "/api"))]
      (is (= (response :status) 200))
      (is (contains? (response :body) :version))))
  (testing "not-found route"
    (let [response (api-routes (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))
