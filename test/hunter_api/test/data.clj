(ns hunter-api.test.data
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [hunter-api.data :refer :all]
            [monger.core :refer [connect get-db]]
            [slingshot.test :refer :all]))

(defn my-test-fixture
  [f]
  (def connection (connect {:host "localhost"
                            :port 27017}))
  (get-db connection "hunter-datasets-test")
  (f))

(use-fixtures :once my-test-fixture)

(deftest test-validation
  (testing "valid dataset ID"
    (is (nil? (validate ["543e62ab40694721af85ae5f" :hunter-api.data/ObjectID]))))
  (testing "invalid dataset ID"
    (is (thrown+? [:type :hunter-api.data/invalid] (validate ["123456789" :hunter-api.data/ObjectID]))))
  (testing "valid dataset"
    (is (nil? (validate [(-> {:foo "Bar"
                              :date 2014}
                             with-oid
                             modify-now
                             create-now) :hunter-api.data/Dataset]))))
  (testing "invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid] (validate [{:foo "Bar"} :hunter-api.data/Dataset])))))

(deftest test-create-dataset
  (testing "create valid dataset"
    (let [ds {:foo "Bar"
              :date 1990
              :zone "France"
              :swag 1}
          created-ds (create-dataset ds "hunter-datasets-test")]
      (is (map? created-ds))
      (is (contains? created-ds :_id))
      (is (contains? created-ds :foo))
      (is (contains? created-ds :date))
      (is (contains? created-ds :zone))
      (is (contains? created-ds :swag))
      (is (contains? created-ds :created))
      (is (contains? created-ds :modified))))
  (testing "create invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid] (create-dataset {} "hunter-datasets-test")))))

(deftest test-get-dataset
  (testing "get valid dataset"
    (let [created (create-dataset {:date 2014})
          ds (get-dataset (.toString (created :_id)))]
      (is (map? ds))
      (is (contains? created :_id))
      (is (contains? created :created))
      (is (contains? created :date))
      (is (contains? created :modified))))
  (testing "get dataset with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid] (get-dataset "666"))))
  (testing "get non-existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found] (get-dataset "543e62ab40694721af85ae5f")))))

(deftest test-delete-dataset
  (testing "delete dataset"
    (let [created (create-dataset {:date 2014})
          deleted (delete-dataset (.toString (created :_id)))]
      (is (not (nil? deleted)))))
  (testing "delete with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid] (delete-dataset "666")))))
