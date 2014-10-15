(ns hunter-api.test.middleware
  (:require [clojure.test :refer :all]
            [hunter-api.middleware :refer :all]
            [ring.mock.request :as mock]
            [slingshot.slingshot :refer [throw+]]))

(deftest test-wrap-exception-handler
  (testing "General Exception Handling"
    (let [handler (wrap-exception-handler (fn [req] (throw+ (Exception. "Server Error"))))
          response (handler (mock/request :get "/api"))]
      (is (= (response :status) 500))))
  (testing "Operation Failed Handling"
    (let [handler (wrap-exception-handler (fn [req] (throw+ {:type :doitnow.data/failed} "500: Failed")))
          response (handler (mock/request :get "/api"))]
      (is (= (response :status) 500))))
  (testing "Invalid Handling"
    (let [handler (wrap-exception-handler (fn [req] (throw+ {:type :doitnow.data/invalid} "400: Bad Request")))
          response (handler (mock/request :get "/api"))]
      (is (= (response :status) 400))))
  (testing "Not Found Handling"
    (let [handler (wrap-exception-handler (fn [req] (throw+ {:type :doitnow.data/not-found} "404: Not Found")))
          response (handler (mock/request :get "/api"))]
      (is (= (response :status) 404)))))
