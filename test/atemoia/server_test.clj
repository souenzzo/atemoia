(ns atemoia.server-test
  (:require [atemoia.server :as atemoia]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [io.pedestal.connector :as conn]
            [io.pedestal.connector.test :refer [response-for]])
  (:import (org.postgresql.util PSQLException)))

(set! *warn-on-reflection* true)

(deftest hello
  (let [atm-db-url (System/getProperty "atemoia.server.atm-db-url"
                     (or (System/getenv "DATABASE_URL")
                       "jdbc:postgresql://0:5432/postgres?user=postgres&password=postgres"))
        conn (-> 8080
               conn/default-connector-map
               (assoc ::atemoia/atm-db-url atm-db-url)
               atemoia/create-connector)
        note (str (random-uuid))]
    (try
      (atemoia/install-schema {::atemoia/atm-conn atm-db-url})
      (catch PSQLException _))
    (is (= true
          (-> conn
            (response-for :get "/todo")
            :body
            (json/parse-string true)
            (->> (filter (comp #{note} :todo/note)))
            empty?))
      "fetching the todos, after creating one")
    (is (= 201
          (-> conn
            (response-for :post "/todo"
              :body (json/generate-string {:note note}))
            :status))
      "creating a todo")
    (is (= false
          (-> conn
            (response-for :get "/todo")
            :body
            (json/parse-string true)
            (->> (filter (comp #{note} :todo/note)))
            empty?))
      "fetching the todos, after creating one")
    (dotimes [idx 10]
      (is (= 201
            (-> conn
              (response-for :post "/todo"
                :body (json/generate-string {:note (str "hello world" idx)}))
              :status))
        (str "creating many todos: idx = " idx)))
    (is (== 10
          (-> conn
            (response-for :get "/todo")
            :body
            (json/parse-string true)
            count))
      "fetching many todos")))
