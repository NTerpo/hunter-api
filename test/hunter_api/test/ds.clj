(ns hunter-api.test.ds)

(def valid-dataset {:title "test"
                    :description "foo bar"
                    :producer "Foo"
                    :temporal-coverage "0666"
                    :spatial-coverage "Bar"
                    :created "0666-01-01"
                    :last-modified "0666-01-02"
                    :uri "http://foo.bar"
                    :tags ["foo" "bar"]})

(def ds1 {:title "test1"
          :description "test1"
          :producer "foo"
          :temporal-coverage "0666"
          :spatial-coverage "Bar"
          :created "0666-01-01"
          :last-modified "0666-01-12"
          :uri "http://www.foo.bar"
          :tags ["foo" "bar"]})

(def ds2 {:title "test2"
          :description "test2"
          :producer "foo"
          :temporal-coverage "0667"
          :spatial-coverage "Bar"
          :created "0666-01-03"
          :last-modified "0666-01-04"
          :uri "http://www.foo.bar"
          :tags ["foo" "bar"]})
