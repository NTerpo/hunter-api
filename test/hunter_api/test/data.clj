(ns hunter-api.test.data
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [hunter-api.data :refer :all]
            [hunter-api.test.ds :refer [valid-dataset]]
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
  (testing "with :created and :last-modified"
    (let [ds (normalize-dates valid-dataset)]
      (is (= (date->valid-date "0666-01-01") (ds :created)))
      (is (= (date->valid-date "0666-01-02") (ds :last-modified)))))
  (testing "without :created and :last-modified"
    (let [ds (normalize-dates {})]
      (is (nil? (ds :created)))
      (is (nil? (ds :last-modified))))))

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
    (let [ds1 (create-dataset {:title "test1"
                               :description "test1"
                               :producer "foo"
                               :temporal-coverage "0666"
                               :spatial-coverage "Bar"
                               :created "0666-01-01"
                               :last-modified "0666-01-12"
                               :uri "http://www.foo.bar"
                               :tags ["foo" "bar"]} api-db-test)
          ds2 (create-dataset {:title "test2"
                               :description "test2"
                               :producer "foo"
                               :temporal-coverage "0667"
                               :spatial-coverage "Bar"
                               :created "0666-01-03"
                               :last-modified "0666-01-04"
                               :uri "http://www.foo.bar"
                               :tags ["foo" "bar"]} api-db-test)
          found (find-dataset {:temporal-coverage "0667"} api-db-test)
          found-2 (find-dataset {:producer "foo"} api-db-test)]
      (is (= (found :title) (ds2 :title)))
      (is (= (found :description) (ds2 :description)))
      (is (= (found :producer) (ds2 :producer)))
      (is (= (found :temporal-coverage) (ds2 :temporal-coverage)))
      (is (= (found :spatial-coverage) (ds2 :spatial-coverage)))
      (is (= (found :created) (ds2 :created)))
      (is (= (found :last-modified) (ds2 :last-modified)))
      (is (= (found :uri) (ds2 :uri)))
      (is (= (found :tags) (ds2 :tags)))
      (is (= (found-2 :title) (ds1 :title)))))
  (testing "not found dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (find-dataset
                   {:temporal-coverage "666"} api-db-test)))))
