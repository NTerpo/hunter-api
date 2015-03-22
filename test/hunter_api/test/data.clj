(ns hunter-api.test.data
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [hunter-api.util :refer :all]
            [hunter-api.data :refer :all]
            [hunter-api.test.ds :refer [valid-dataset ds1 ds2 ds3 ds-rand]]
            [monger.core :refer [connect get-db]]
            [slingshot.test :refer :all]))

(def api-db-test "hunter-datasets-test")

(deftest test-date->valid-date
  (testing "YYYY-MM-dd"
    (is (= "1937-07-18"
           (f/unparse multi-parser (date->valid-date "1937-07-18")))))
  (testing "YYYY/MM/dd"
    (is (= "1937-07-18"
           (f/unparse multi-parser (date->valid-date "1937/07/18")))))
  (testing "invalid date format"
    (is (thrown? IllegalArgumentException
                 (f/unparse multi-parser (date->valid-date "19370718"))))))

(deftest test-normalize-dates
  (testing "with :created and :updated"
    (let [ds (normalize-dates valid-dataset)]
      (is (= (date->valid-date "0666-01-01")
             (ds :created)))
      (is (= (date->valid-date "0666-01-02")
             (ds :updated)))))
  (testing "without :created and :updated"
    (let [ds (normalize-dates {})]
      (is (empty? ds)))))

(deftest test-validation
  (testing "valid dataset ID"
    (is (nil? (validate ["543e62ab40694721af85ae5f" :hunter-api.data/ObjectID]))))
  (testing "invalid dataset ID"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (validate ["123456789" :hunter-api.data/ObjectID]))))
  (testing "valid dataset"
    (is (nil? (validate [(-> valid-dataset
                             with-oid
                             modify-now
                             create-now) :hunter-api.data/Dataset]))))
  (testing "invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (validate [{:foo "Bar"} :hunter-api.data/Dataset])))))

(deftest test-create-dataset
  (testing "create valid dataset"
    (let [created-ds (create-dataset valid-dataset api-db-test)]
      (is (map? created-ds))
      (is (nil? (validate [created-ds :hunter-api.data/Dataset])))))
  (testing "create invalid dataset"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (create-dataset {} api-db-test)))))

(deftest test-index-dataset
  (testing "index valid dataset"
    )
  (testing "index invalid dataset"
    )) ;; TODO

(deftest test-get-dataset
  (testing "get valid dataset"
    (let [created-ds (create-dataset valid-dataset api-db-test)
          ds (get-dataset (.toString (created-ds :_id)) api-db-test)]
      (is (map? ds))
      (is (nil? (validate [ds :hunter-api.data/Dataset])))
      (is (= (created-ds ds)))))
  (testing "get dataset with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (get-dataset "666" api-db-test))))
  (testing "get non-existent dataset"
    (is (thrown+? [:type :hunter-api.data/not-found] (get-dataset "543e62ab40694721af85ae5f" api-db-test)))))

(deftest test-delete-dataset
  (testing "delete dataset"
    (let [created-ds (create-dataset valid-dataset api-db-test)
          deleted (delete-dataset (.toString (created-ds :_id)) api-db-test)]
      (is (not (nil? deleted)))))
  (testing "delete with invalid id"
    (is (thrown+? [:type :hunter-api.data/invalid]
                  (delete-dataset "666" api-db-test)))))

(deftest test-update-dataset
  (testing "update a dataset"
    (let [ds (create-dataset (ds-rand) api-db-test)
          ds-title (ds :title)
          up (update-dataset {:title ds-title}
                      {:huntscore 99 :new-field "aupa BO"}
                      api-db-test)
          updated-ds (first (find-dataset {:title ds-title} api-db-test))]
      
      (is (= 99
             (updated-ds :huntscore)))
      (is (= "aupa BO"
             (updated-ds :new-field)))))
  (testing "not enough arguments"
    (let [ds1 (create-dataset ds1 api-db-test)
          ds2 (create-dataset ds2 api-db-test)
          ds3 (create-dataset ds3 api-db-test)]
      (is (thrown+? [:type :hunter-api.data/invalid]
                    (update-dataset {:publisher "foo"}
                                     {:updated "0777-01-05" :new-field "aupa BO"} api-db-test))))))

(deftest test-find-dataset
  (testing "finding a dataset"
    (let [ds1 (create-dataset ds1 api-db-test)
          ds2 (create-dataset ds2 api-db-test)
          ds3 (create-dataset ds3 api-db-test)
          found (last (find-dataset {:title "test2"} api-db-test))
          found-2 (last (find-dataset {:spatial "baz"} api-db-test))
          found-3 (last (find-dataset {:publisher "foo"} api-db-test))]
      (is (= (ds2 :title)
             (found :title)))
      (is (= (ds2 :title)
             (found-2 :title)))
      (is (= (ds1 :title)
             (found-3 :title)))))
  (testing "not found dataset"
    (is (thrown+? [:type :hunter-api.data/not-found]
                  (find-dataset
                   {:temporal "666"}
                   api-db-test)))))
