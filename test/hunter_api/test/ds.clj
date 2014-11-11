(ns hunter-api.test.ds)

(def valid-dataset {:title "test"
                    :description "foo bar"
                    :publisher "Foo"
                    :temporal "0666"
                    :spatial "Bar"
                    :created "0666-01-01"
                    :updated "0666-01-02"
                    :uri "http://foo.bar"
                    :tags ["foo" "bar"]})

(def ds1 {:title "test1"
          :description "test1"
          :publisher "foo"
          :temporal "0666"
          :spatial "Bar"
          :created "0666-01-01"
          :updated "0666-01-12"
          :uri "http://www.foo.bar"
          :tags ["foo" "bar"]
          :huntscore 25})

(def ds2 {:title "test2"
          :description "test2"
          :publisher "foo"
          :temporal "0667"
          :spatial "baz"
          :created "0666-01-03"
          :updated "0666-01-04"
          :uri "http://www.foo.bar"
          :tags ["foo" "bar"]
          :huntscore 25})

(def ds3 {:title "test2"
          :description "test2"
          :publisher "foo"
          :temporal "0667"
          :spatial "baz"
          :created "0666-01-03"
          :updated "0666-01-04"
          :uri "http://www.foo.bar"
          :tags ["foo" "bar"]
          :huntscore 2})
