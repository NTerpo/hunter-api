(ns hunter-api.config
  (:require [monger.core :refer [connect-via-uri]]))

(def config
  (let [{:keys [conn db]} (connect-via-uri "mongodb://terpo:Hunter666@dogen.mongohq.com:10036/app31566584")]
    {:conn conn
     :db db
     :db-name "app31566584"}))
