(ns hunter-api.test.handler
  (:require [clojure.test :refer :all]
            [hunter-api.handler :refer :all]
            [hunter-api.test.ds :refer [valid-dataset ds1 ds2]]
            [hunter-api.data :refer [delete-dataset]]
            [cheshire.core :refer :all]
            [ring.mock.request :as mock]
            [clj-http.client :as client]
            [slingshot.test :refer :all]))

(deftest test-api-routes
  (testing "API options"
    (let [response (api-routes (mock/request :options "/api"))]
      (is (= (response :status) 200))
      (is (contains? (response :body) :version))))
  (testing "not-found route"
    (let [response (api-routes (mock/request :get "/invalid"))]
      (is (= (:status response) 404))))
  (testing "API get"
    (let [response (api-routes (mock/request :get "/api"))]
      (is (= (response :status) 405))
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
      (is (= (response :status) 201))
      (is (contains? response-headers "Location"))
      (is (map? response-body))
      (is (contains? response-body :_id))
      (is (contains? response-body :created-ds))
      (is (contains? response-body :modified-ds))
      (is (contains? response-body :title))
      (is (= (response-body :title) "test"))
      (is (contains? response-body :description))
      (is (contains? response-body :publisher))
      (is (contains? response-body :temporal))
      (is (contains? response-body :spatial))
      (is (contains? response-body :created))
      (is (contains? response-body :updated))
      (is (contains? response-body :uri))
      (is (contains? response-body :tags))
      (delete-dataset (.toString (response-body :_id)) api-db)
      )))

(deftest test-get-dataset
  (testing "get valid dataset with valid id"
    (let [response (json-request valid-dataset)
          id (.toString (:_id (parse-string (response :body) true)))]
      (is (= (response :status) 201))
      (let [response (api-routes (mock/request :get (str "/api/datasets/" id)))
            response-body (response :body)]
        (is (= (response :status) 200))
        (is (map? response-body))
        (is (contains? response-body :_id))
        (is (contains? response-body :created-ds))
        (is (contains? response-body :modified-ds))
        (is (contains? response-body :title))
        (is (= (response-body :title) "test"))
        (is (contains? response-body :description))
        (is (contains? response-body :publisher))
        (is (contains? response-body :temporal))
        (is (contains? response-body :spatial))
        (is (contains? response-body :created))
        (is (contains? response-body :updated))
        (is (contains? response-body :uri))
        (is (contains? response-body :tags)))
      (delete-dataset id api-db)))
  (testing "get with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (api-routes (mock/request :get "/api/datasets/666")))))
  (testing "get non existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes (mock/request :get "/api/datasets/543e62ab40694721af85ae5f"))))))

(deftest test-delete-dataset
  (testing "delete valid dataset"
    (let [response (json-request valid-dataset)
          id (.toString (:_id (parse-string (response :body) true)))]
      (is (= (response :status) 201))
      (let [response (api-routes (mock/request :delete (str "/api/datasets/" id)))
            response-body (response :body)]
        (is (= (response :status) 200)))))
  (testing "delete dataset with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (api-routes (mock/request :delete "/api/datasets/666")))))
  (testing "delete non existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes (mock/request :delete "/api/datasets/543e62ab40694721af85ae5f"))))))

(deftest test-find-dataset
  (testing "finding a dataset"
    (let [response-1 (json-request ds1)
          response-2 (json-request ds2)
          id-1 (.toString (:_id (parse-string (response-1 :body) true)))
          id-2 (.toString (:_id (parse-string (response-2 :body) true)))]
      (let [found-1 (api-routes
                     (mock/request :get "/api/datasets/?description=test2"))
            found-2 (api-routes
                     (mock/request :get "/api/datasets/?publisher=foo"))]
        (is (= (found-1 :title) (response-2 :title)))
        (is (= (found-1 :description) (response-2 :description)))
        (is (= (found-1 :publisher) (response-2 :publisher)))
        (is (= (found-1 :temporal) (response-2 :temporal)))
        (is (= (found-1 :spatial) (response-2 :spatial)))
        (is (= (found-1 :created) (response-2 :created)))
        (is (= (found-1 :updated) (response-2 :updated)))
        (is (= (found-1 :uri) (response-2 :uri)))
        (is (= (found-1 :tags) (response-2 :tags)))
        (is (= (found-2 :title) (response-1 :title))))
      (delete-dataset id-1 api-db)
      (delete-dataset id-2 api-db)))
  (testing "not found dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes
                   (mock/request :get "/api/datasets/?uri=666"))))))
