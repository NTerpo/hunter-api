(ns hunter-api.test.handler
  (:require [clojure.test :refer :all]
            [hunter-api.handler :refer :all]
            [hunter-api.test.ds :refer [valid-dataset ds1 ds2 ds3]]
            [hunter-api.data :refer [delete-dataset]]
            [cheshire.core :refer :all]
            [ring.mock.request :as mock]
            [clj-http.client :as client]
            [slingshot.test :refer :all]))

(deftest test-api-routes
  (testing "API options"
    (let [response (api-routes (mock/request :options "/api"))]
      (is (= 200
             (response :status)))
      (is (contains? (response :body) :version))))
  (testing "not-found route"
    (let [response (api-routes (mock/request :get "/invalid"))]
      (is (= 404
             (:status response)))))
  (testing "API get"
    (let [response (api-routes (mock/request :get "/api"))]
      (is (= 405
             (response :status)))
      (is (nil? (response :body))))))

(defn- json-request
  [body]
  (client/post
   "http://localhost:3000/api/datasets"
   {:body (generate-string body)
    :content-type "application/json"}))

(deftest test-create-dataset
  (testing "create a valid dataset"
    (let [response (json-request valid-dataset)
          response-body (parse-string (response :body) true)
          response-headers (response :headers)]
      (is (= 201
             (response :status)))
      (is (contains? response-headers "Location"))
      (is (map? response-body))
      (is (contains? response-body :_id))
      (is (contains? response-body :created-ds))
      (is (contains? response-body :modified-ds))
      (is (contains? response-body :title))
      (is (= "test"
             (response-body :title)))
      (is (contains? response-body :description))
      (is (contains? response-body :publisher))
      (is (contains? response-body :temporal))
      (is (contains? response-body :spatial))
      (is (contains? response-body :created))
      (is (contains? response-body :updated))
      (is (contains? response-body :uri))
      (is (contains? response-body :tags))
      (delete-dataset (.toString (response-body :_id))))))

(deftest test-get-dataset
  (testing "get valid dataset with valid id"
    (let [response (json-request valid-dataset)
          id (.toString (:_id (parse-string (response :body) true)))]
      (is (= 201
             (response :status)))
      (let [response (api-routes (mock/request :get (str "/api/datasets/" id)))
            response-body (response :body)]
        (is (= 200
               (response :status)))
        (is (map? response-body))
        (is (contains? response-body :_id))
        (is (contains? response-body :title))
        (is (= "test"
               (response-body :title))))
      (delete-dataset id)))
  (testing "get with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (api-routes (mock/request :get "/api/datasets/666")))))
  (testing "get non existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes
                   (mock/request :get
                                 "/api/datasets/543e62ab40694721af85ae5f"))))))

(deftest test-delete-dataset
  (testing "delete valid dataset"
    (let [response (json-request valid-dataset)
          id (.toString (:_id (parse-string (response :body) true)))]
      (is (= 201
             (response :status)))
      (let [response (api-routes (mock/request :delete (str "/api/datasets/" id)))
            response-body (response :body)]
        (is (= 200
               (response :status))))))
  (testing "delete dataset with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (api-routes
                   (mock/request :delete
                                 "/api/datasets/666")))))
  (testing "delete non existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes
                   (mock/request :delete
                                 "/api/datasets/543e62ab40694721af85ae5f"))))))

(deftest test-find-dataset
  (testing "finding a dataset"
    (let [response-1 (json-request ds1)
          response-2 (json-request ds2)
          response-3 (json-request ds3)
          id-1 (.toString (:_id (parse-string (response-1 :body) true)))
          id-2 (.toString (:_id (parse-string (response-2 :body) true)))
          id-3 (.toString (:_id (parse-string (response-3 :body) true)))]
      (let [found-1 (api-routes
                     (mock/request :get
                                   "/api/datasets/?title=test2"))
            found-2 (api-routes
                     (mock/request :get
                                   "/api/datasets/?spatial=baz"))
            found-3 (api-routes
                     (mock/request :get
                                   "/api/datasets/?publisher=foo"))]
        (is (= (response-2 :title)
               (found-1 :title)))
        (is (= (response-2 :title)
               (found-2 :title)))
        (is (= (response-1 :title)
               (found-3 :title))))
      (delete-dataset id-1)
      (delete-dataset id-2)
      (delete-dataset id-3)))
  (testing "not found dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes
                   (mock/request :get
                                 "/api/datasets/?uri=666"))))))
