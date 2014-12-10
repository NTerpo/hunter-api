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

(def alphanumeric "abcdefghijklmnopqrstuvwxyz1234567890")

(defn get-random-title [length]
  (apply str (repeatedly length #(rand-nth alphanumeric))))

(defn ds-rand []
  {:title (get-random-title 5)
   :description "abc"
   :publisher "def"
   :temporal "0099"
   :spatial "yep"
   :created "0698-01-03"
   :updated "0786-01-04"
   :uri "http://www.oofoo.bar"
   :tags ["oofoo" "oobar"]
   :huntscore 9})
