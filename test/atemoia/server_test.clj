(ns atemoia.server-test
  (:require [atemoia.server :as atemoia]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.test :refer [response-for]]
            [next.jdbc :as jdbc])
  (:import (clojure.lang IDeref)
           (java.lang AutoCloseable)
           (java.net URI)
           (java.util UUID)))

(defn temp-jdbc-database
  [base-uri]
  (let [uri (URI/create (.getSchemeSpecificPart (URI/create base-uri)))
        new-db (string/replace (str "testdb-" (UUID/randomUUID))
                 "-" "_")
        new-uri (URI. (.getScheme uri) (.getUserInfo uri)
                  (.getHost uri) (.getPort uri)
                  (str "/" new-db)
                  (.getQuery uri)
                  (.getFragment uri))
        new-url (str (URI. "jdbc" (str new-uri) nil))
        conn {:jdbcUrl new-url}]
    (jdbc/execute! {:jdbcUrl base-uri}
      [(str "CREATE DATABASE " new-db)])
    (reify IDeref
      (deref [this] conn)
      AutoCloseable
      (close [this]
        (jdbc/execute! {:jdbcUrl base-uri}
          [(str "DROP DATABASE " new-db)])))))

(deftest hello
  (with-open [*atm-conn (temp-jdbc-database "jdbc:postgresql://127.0.0.1:5432/postgres?user=postgres&password=postgres")]
    (atemoia/install-schema {::atemoia/atm-conn @*atm-conn})
    (let [service-fn (-> {::http/routes atemoia/routes}
                       http/default-interceptors
                       (update ::http/interceptors
                         (partial cons (interceptor/interceptor
                                         {:enter (fn [ctx]
                                                   (assoc-in ctx [:request ::atemoia/atm-conn]
                                                     @*atm-conn))})))
                       #_http/dev-interceptors
                       http/create-servlet
                       ::http/service-fn)]
      (is (= []
            (-> service-fn
              (response-for :get "/todo")
              :body
              (json/parse-string true))))
      (is (= 201
            (-> service-fn
              (response-for :post "/todo"
                :body (json/generate-string {:note "hello world"}))
              :status)))
      (is (= [{:todo/id   1
               :todo/note "hello world"}]
            (-> service-fn
              (response-for :get "/todo")
              :body
              (json/parse-string true)))))))
