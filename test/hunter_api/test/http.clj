(ns hunter-api.test.http
  (:require [clojure.test :refer :all]
            [hunter-api.http :refer :all]
            [ring.mock.request :refer :all]))

(deftest test-url-from
  (testing "Create basic url"
    (let [url (url-from {:server-name "localhost" :server-port "8080"})]
      (is (string? url))
      (is (= "http://localhost:8080/"
             url))))
  (testing "Create basic url"
    (let [url (url-from {:server-name "localhost" :server-port "8080" :uri "/api/datasets"})]
      (is (string? url))
      (is (= "http://localhost:8080/api/datasets/"
             url))))
  (testing "create complete url"
    (let [url (url-from {:server-name "localhost" :server-port "8080" :uri "/api/datasets"} "543e62ab40694721af85ae5f" "field")]
      (is (string? url))
      (is (= "http://localhost:8080/api/datasets/543e62ab40694721af85ae5f/field"
             url)))))

(deftest test-options
  (testing "HTTP options default response"
    (let [response (options)]
      (is (= 200
             (response :status)))
      (is (nil? (response :body)))
      (is (= (get-in response [:headers "Allow"] "OPTIONS")))))
  (testing "HTTP options with allowed response"
    (let [response (options [:get :post])]
      (is (= 200
             (response :status)))
      (is (nil? (response :body)))
      (is (= (get-in response [:headers "Allow"] "Get, POST")))))
  (testing "HTTP options with body response"
    (let [response (options [:get :post] {:version "version-number"})]
      (is (= 200
             (response :status)))
      (is (map? (response :body)))
      (is (contains? (response :body) :version))
      (is (= (get-in response [:headers "Allow"] "GET, POST"))))))

(deftest test-method-not-allowed
  (testing "HTTP method not allowed with options"
    (let [response (method-not-allowed [:options :get])]
      (is (= 405
             (response :status)))
      (is (nil? (response :body)))
      (is (= (get-in response [:headers "Allow"] "OPTIONS, GET"))))))

(deftest test-no-content?
  (testing "HTTP no-content nil body"
    (let [response (no-content? nil)]
      (is (= 204
             (response :status)))
      (is (nil? (response :body)))))
  (testing "HTTP no-content empty body"
    (let [response (no-content? {})]
      (is (= 204
             (response :status)))
      (is (nil? (response :body)))))
  (testing "HTTP no-content not-a-sequence body"
    (let [response (no-content? "string")]
      (is (= 200
             (response :status))))))

(deftest test-not-implemented
  (testing "HTTP not implemented"
    (let [response (not-implemented)]
      (is (= 501
             (response :status)))
      (is (nil? (response :body))))))

(deftest test-created
  (testing "create with location"
    (let [response (created (url-from (request :post "/api/datasets") "543e62ab40694721af85ae5f"))
          location (get-in response [:headers "location"])]
      (is (= 201
             (response :status)))
      (is (= "http://localhost:80/api/datasets/543e62ab40694721af85ae5f"
             location))
      (is (nil? (response :body)))))
  (testing "create with location and body"
    (let [response (created (url-from (request :post "/api/datasets") "543e62ab40694721af85ae5f") {:title "test"})
          location (get-in response [:headers "location"])
          body (response :body)]
      (is (= 201
             (response :status)))
      (is (= "http://localhost:80/api/datasets/543e62ab40694721af85ae5f"
             location))
      (is (map? body)))))

(deftest test-ok
  (testing "HTTP ok"
    (let [response (ok {:title "test"})
          body (response :body)]
      (is (= 200
             (response :status)))
      (is (map? body)))))
