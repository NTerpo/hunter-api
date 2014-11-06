(ns hunter-api.test.data
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [hunter-api.data :refer :all]
            [hunter-api.test.ds :refer [valid-dataset ds1 ds2]]
            [monger.core :refer [connect get-db]]
            [slingshot.test :refer :all]))

(def api-db-test "hunter-datasets-test")

(deftest test-date->valid-date
  (testing "YYYY-MM-dd"
    (is (= "1937-07-18" (f/unparse multi-parser (date->valid-date "1937-07-18")))))
  (testing "YYYY/MM/dd"
    (is (= "1937-07-18" (f/unparse multi-parser (date->valid-date "1937/07/18")))))
  (testing "invalid date format"
    (is (thrown? IllegalArgumentException
                 (f/unparse multi-parser (date->valid-date "19370718"))))))

(deftest test-normalize-dates
  (testing "with :created and :updated"
    (let [ds (normalize-dates valid-dataset)]
      (is (= (date->valid-date "0666-01-01") (ds :created)))
      (is (= (date->valid-date "0666-01-02") (ds :updated)))))
  (testing "without :created and :updated"
    (let [ds (normalize-dates {})]
      (is (empty? ds)))))

(deftest test-validation
  (testing "valid dataset ID"
    (is (nil? (validate ["543e62ab40694721af85ae5f" :hunter-api.data/ObjectID]))))
  (testing "invalid dataset ID"
    (is (thrown+? [:type :hunter-api.data/invalid] (validate ["123456789" :hunter-api.data/ObjectID]))))
  (testing "valid dataset"
    (is (nil? (validate [(-> valid-dataset
                             with-oid
                             modify-now
                             create-now) :hunter-api.data/Dataset]))))
  (testing "invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid] (validate [{:foo "Bar"} :hunter-api.data/Dataset])))))

(deftest test-create-dataset
  (testing "create valid dataset"
    (let [created-ds (create-dataset valid-dataset api-db-test)]
      (is (map? created-ds))
      (is (nil? (validate [created-ds :hunter-api.data/Dataset])))))
  (testing "create invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid] (create-dataset {} api-db-test)))))

(deftest test-get-dataset
  (testing "get valid dataset"
    (let [created-ds (create-dataset valid-dataset api-db-test)
          ds (get-dataset (.toString (created-ds :_id)) api-db-test)]
      (is (map? ds))
      (is (nil? (validate [ds :hunter-api.data/Dataset])))
      (is (= (created-ds ds)))))
  (testing "get dataset with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid] (get-dataset "666" api-db-test))))
  (testing "get non-existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found] (get-dataset "543e62ab40694721af85ae5f" api-db-test)))))

(deftest test-delete-dataset
  (testing "delete dataset"
    (let [created-ds (create-dataset valid-dataset api-db-test)
          deleted (delete-dataset (.toString (created-ds :_id)) api-db-test)]
      (is (not (nil? deleted)))))
  (testing "delete with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid] (delete-dataset "666" api-db-test)))))

(deftest test-find-dataset
  (testing "finding a dataset"
    (let [ds1 (create-dataset ds1  api-db-test)
          ds2 (create-dataset  ds2 api-db-test)
          found (last (find-dataset {:temporal "0667"} api-db-test))
          found-2 (last (find-dataset {:publisher "foo"} api-db-test))]
      (is (= (found :title) (ds2 :title)))
      (is (= (found :description) (ds2 :description)))
      (is (= (found :publisher) (ds2 :publisher)))
      (is (= (found :temporal) (ds2 :temporal)))
      (is (= (found :spatial) (ds2 :spatial)))
      (is (= (found :created) (ds2 :created)))
      (is (= (found :updated) (ds2 :updated)))
      (is (= (found :uri) (ds2 :uri)))
      (is (= (found :tags) (ds2 :tags)))
      (is (= (found-2 :title) (ds1 :title)))))
  (testing "not found dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (find-dataset
                   {:temporal "666"} api-db-test)))))
