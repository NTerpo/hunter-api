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

(def valid-dataset {:title "Campagne 2001 de recensements nationaux - Population par sexe, âge, type de ménage et situation du ménage"
                              :description "Ménages, Population par sexe, âge, type de ménage et situation du ménage"
                              :producer "Eurostat"
                              :temporal-coverage 2001
                              :spatial-coverage "France"
                              :created "2013-09-18"
                              :last-modified "2014-09-17"
                    :uri "http://www.data-publica.com/opendata/9980--campagne-2001-de-recensements-nationaux-population-par-sexe-age-type-de-menage-et-situation-du-menage-2001"
                    :tags ["population" "survey"]})

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
