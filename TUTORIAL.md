# Creating a full-stack clojure app

# Create a project

```shell
mkdir atemoia
cd atemoia
echo {} > deps.edn
mkdir dev src
```

`deps.edn`

```clojure
{:paths   ["src"]
 :deps    {com.github.seancorfield/next.jdbc {:mvn/version "1.2.674"}
           hiccup/hiccup                     {:mvn/version "2.0.0-alpha2"}
           io.pedestal/pedestal.jetty        {:mvn/version "0.5.9"}
           io.pedestal/pedestal.service      {:mvn/version "0.5.9"}
           org.clojure/clojure               {:mvn/version "1.10.3"}
           org.postgresql/postgresql         {:mvn/version "42.2.23"}}
 :aliases {:dev {:extra-paths ["dev"]
                 :extra-deps  {io.github.clojure/tools.build {:git/url "https://github.com/clojure/tools.build.git"
                                                              :sha     "1e7c019730dc6f9e38793170c8801c5950516b60"}
                               reagent/reagent               {:mvn/version "1.1.0"}
                               com.google.guava/guava        {:mvn/version "30.1.1-jre"}
                               thheller/shadow-cljs          {:mvn/version "2.15.2"}}}}}
```

# Create a backend

```shell
mkdir src/atemoia
echo '(ns atemoia.server)' > src/atemoia/server.clj
```

> A minimium http server
`server.clj`

```clojure
(ns atemoia.server
  (:require [io.pedestal.http :as http]))

(defn index
  [_]
  {:body   "Hello"
   :status 200})
(def routes
  `#{["/" :get index]})

(defonce state
  (atom nil))

(defn -main
  [& _]
  (swap! state
    (fn [st]
      (some-> st http/stop)
      (-> {::http/port   8080
           ::http/type   :jetty
           ::http/routes routes
           ::http/join?  false}
        http/default-interceptors
        http/create-server
        http/start))))
```

> Add hiccup
`server.clj`

```clojure
(ns atemoia.server
  (:require [io.pedestal.http :as http]
    [hiccup2.core :as h]))

(defn index
  [_]
  (let [html [:html
              {:lang "en"}
              [:head
               [:meta {:charset "UTF-8"}]
               [:title "atemoia"]]
              [:body
               [:div "hello"]]]]
    {:body    (->> html
                (h/html {:mode :html})
                (str "<!DOCTYPE html>\n"))
     :headers {"Content-Security-Policy" ""
               "Content-Type"            "text/html"}
     :status  200}))

```

# Add cljs support

```shell
echo {} > package.json
echo {} > shadow-cljs.edn
echo '(ns atemoia.client)' > src/atemoia/client.cljs
npm install --save react react-dom --save-dev shadow-cljs@2.15.2
```

`shadow-cljs.edn`

```clojure
{:deps   {:aliases [:dev]}
 :builds {:atemoia {:target     :browser
                    :output-dir "target/classes/public/atemoia"
                    :asset-path "/atemoia"
                    :modules    {:main {:init-fn atemoia.client/start}}
                    :devtools   {:after-load atemoia.client/after-load}}}}
```

`client.cljs`

```clojure
(ns atemoia.client)
(defn start
  []
  (prn :start))
(defn after-load
  []
  (prn :after-load))
```

> start shadow-cljs

`server.clj`

```clojure
(defn dev-main
  [& _]
  (-> `shadow.cljs.devtools.server/start!
    requiring-resolve
    (apply []))
  (-> `shadow.cljs.devtools.api/watch
    requiring-resolve
    (apply [:atemoia]))
  (-main))
```

> run and see the output

```
target/
└── classes
    └── public
        └── atemoia
            ├── main.js
            └── manifest.edn
```

- Ok, we need to serve the `target/classes/public` folder

`server.cljs`

```clojure
(-> {::http/port      8080
     ::http/type      :jetty
     ::http/routes    routes
     ::http/file-path "target/classes/public"
     ::http/join?     false})
```

- Now we can require our cljs

```clojure
[:body
 [:div
  {:id "atemoia"}
  "hello"]
 [:script
  {:src "/atemoia/main.js"}]]
```

> A todo list in UI-only

> My first endpoint

> Connect to JDBC

> All endpoints

> Back to UI

> Build script.

> We are done

