(ns hunter-api.test.handler
  (:require [clojure.test :refer :all]
            [hunter-api.handler :refer :all]
            [hunter-api.test.ds :refer [valid-dataset ds1 ds2]]
            [hunter-api.data :refer [delete-dataset]]
            [ring.mock.request :as mock]
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

(deftest test-create-dataset
  (testing "create a valid dataset"
    (let [response (api-routes
                    (-> (mock/request :post "/api/datasets")
                        (assoc :body valid-dataset)))
          response-body (response :body)
          response-headers (response :headers)]
      (is (= (response :status) 201))
      (is (contains? response-headers "location"))
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
      (delete-dataset (.toString (response-body :_id)) api-db))))

(deftest test-get-dataset
  (testing "get valid dataset with valid id"
    (let [response (api-routes
                    (-> (mock/request :post "/api/datasets")
                        (assoc :body valid-dataset)))
          id (.toString (:_id (response :body)))]
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
    (let [response (api-routes
                    (-> (mock/request :post "/api/datasets")
                        (assoc :body valid-dataset)))
          id (.toString (:_id (response :body)))]
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
    (let [response-1 (api-routes
                      (-> (mock/request :post "/api/datasets")
                          (assoc :body ds1)))
          response-2 (api-routes
                      (-> (mock/request :post "/api/datasets")
                          (assoc :body ds2)))
          id-1 (.toString (:_id (response-1 :body)))
          id-2 (.toString (:_id (response-2 :body)))]
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
