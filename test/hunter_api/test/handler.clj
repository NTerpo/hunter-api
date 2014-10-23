(ns hunter-api.test.handler
  (:require [clojure.test :refer :all]
            [hunter-api.handler :refer :all]
            [hunter-api.test.ds :refer [valid-dataset]]
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
      (is (= (response-body :title) "Campagne 2001 de recensements nationaux"))
      (is (contains? response-body :description))
      (is (contains? response-body :producer))
      (is (contains? response-body :temporal-coverage))
      (is (contains? response-body :spatial-coverage))
      (is (contains? response-body :created))
      (is (contains? response-body :last-modified))
      (is (contains? response-body :uri))
      (is (contains? response-body :tags)))))

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
      (is (= (response-body :title) "Campagne 2001 de recensements nationaux"))
      (is (contains? response-body :description))
      (is (contains? response-body :producer))
      (is (contains? response-body :temporal-coverage))
      (is (contains? response-body :spatial-coverage))
      (is (contains? response-body :created))
      (is (contains? response-body :last-modified))
      (is (contains? response-body :uri))
      (is (contains? response-body :tags)))))
  (testing "get with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (api-routes (mock/request :get "/api/datasets/666")))))
  (testing "get non existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (api-routes (mock/request :get "/api/datasets/543e62ab40694721af85ae5f"))))))
