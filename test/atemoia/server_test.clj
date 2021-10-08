(ns atemoia.server-test
  (:require [clojure.test :refer [deftest is testing]]
            [atemoia.server :as atemoia]
            [io.pedestal.test :refer [response-for]]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor :as interceptor]
            [cheshire.core :as json])
  (:import (java.sql ResultSetMetaData ResultSet PreparedStatement Connection)
           (javax.sql DataSource)))

(defn mock-jdbc
  [handler]
  (reify
    DataSource
    (getConnection [this]
      this)
    Connection
    (prepareStatement [this sql]
      (let [*result (delay
                      (vec (handler sql)))
            *ks (delay (vec (keys (first @*result))))
            *columns (delay (mapv name @*ks))
            *tables (delay (mapv namespace @*ks))
            *n (delay (count @*result))
            *row (atom -1)]
        (reify PreparedStatement
          (close [this])
          (execute [this]
            @*result
            true)
          (getResultSet [this]
            (reify ResultSet
              (next [this]
                (let [n (swap! *row inc)]
                  (< n @*n)))
              (getObject [this ^int n]
                (get (get @*result @*row)
                  (get @*ks (dec n))))

              (getMetaData [this]
                (reify ResultSetMetaData
                  (getTableName [this n]
                    (get @*tables (dec n)))
                  (getColumnLabel [this n]
                    (get @*columns (dec n)))
                  (getColumnCount [this]
                    (count @*ks)))))))))))

(defn service-with
  [update-request]
  (-> {::http/routes atemoia/routes}
    http/default-interceptors
    (update ::http/interceptors
      (partial cons (interceptor/interceptor
                      {:enter (fn [ctx]
                                (update ctx :request update-request))})))
    http/create-servlet
    ::http/service-fn))

(deftest hello
  (let [*sqls (atom [])
        service-fn (service-with
                     (fn [request]
                       (assoc request
                         ::atemoia/atm-conn (mock-jdbc (fn [sql]
                                                         (swap! *sqls conj sql)
                                                         [{:todo/id   0
                                                           :todo/note "hello"}
                                                          {:todo/id   1
                                                           :todo/note "world"}])))))]
    (is (= [{:todo/id   0
             :todo/note "hello"}
            {:todo/id   1
             :todo/note "world"}]
          (-> service-fn
            (response-for :get "/todo")
            :body
            (json/parse-string true))))
    (is (= ["SELECT * FROM todo"]
          @*sqls))))
