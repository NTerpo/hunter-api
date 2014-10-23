(ns hunter-api.test.data
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [hunter-api.data :refer :all]
            [hunter-api.test.ds :refer [valid-dataset]]
            [monger.core :refer [connect get-db]]
            [slingshot.test :refer :all]))

(defn my-test-fixture
  [f]
  (def connection (connect {:host "localhost"
                            :port 27017}))
  (get-db connection "hunter-datasets-test")
  (f))

(use-fixtures :once my-test-fixture)

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
      (is (= (date->valid-date "2013-09-18") (ds :created)))
      (is (= (date->valid-date "2014-09-17") (ds :last-modified)))))
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
    (let [created-ds (create-dataset valid-dataset "hunter-datasets-test")]
      (is (map? created-ds))
      (is (nil? (validate [created-ds :hunter-api.data/Dataset])))))
  (testing "create invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid] (create-dataset {} "hunter-datasets-test")))))

(deftest test-get-dataset
  (testing "get valid dataset"
    (let [created-ds (create-dataset valid-dataset "hunter-datasets-test")
          ds (get-dataset (.toString (created-ds :_id)) "hunter-datasets-test")]
      (is (map? ds))
      (is (nil? (validate [ds :hunter-api.data/Dataset])))
      (is (= (created-ds ds)))))
  (testing "get dataset with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid] (get-dataset "666"))))
  (testing "get non-existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found] (get-dataset "543e62ab40694721af85ae5f")))))

(deftest test-delete-dataset
  (testing "delete dataset"
    (let [created-ds (create-dataset valid-dataset "hunter-datasets-test")
          deleted (delete-dataset (.toString (created-ds :_id)) "hunter-datasets-test")]
      (is (not (nil? deleted)))))
  (testing "delete with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid] (delete-dataset "666")))))

(deftest test-find-dataset
  (testing "finding a dataset"
    (let [ds1 (create-dataset {:title "foo"
                               :description "Aupa BO"
                               :producer "BO"
                               :temporal-coverage "2001"
                               :spatial-coverage "Eus"
                               :created "2013-09-18"
                               :last-modified "2014-09-25"
                               :uri "http://www.data.eus"
                               :tags ["population" "survey"]} "hunter-datasets-test")
          ds2 (create-dataset {:title "bar"
                               :description "miarritzeko"
                               :producer "BO"
                               :temporal-coverage "2004"
                               :spatial-coverage "Eus"
                               :created "2013-09-18"
                               :last-modified "2014-09-19"
                               :uri "http://www.data.eus"
                               :tags ["population" "survey"]} "hunter-datasets-test")
          found (find-dataset {:temporal-coverage "2004"} "hunter-datasets-test")
          found-2 (find-dataset {:producer "BO"} "hunter-datasets-test")]
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
                  (find-dataset {:temporal-coverage "666"} "hunter-datasets-test")))))
