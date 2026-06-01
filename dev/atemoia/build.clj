(ns atemoia.build
  (:require [clojure.tools.build.api :as b]
            [shadow.cljs.devtools.api :as shadow.api]
            [shadow.cljs.devtools.server :as shadow.server]))

(def lib 'atemoia/app)
(def class-dir "target/classes")
(def uber-file "target/atemoia.jar")

(defn build
  []
  (let [basis (b/create-basis)]
    (b/delete {:path "target"})
    (shadow.server/start!)
    (shadow.api/release! :atemoia)
    (shadow.server/stop!)
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   "1.0.0"
                  :basis     basis})
    #_(b/copy-dir {:src-dirs   (:paths basis)
                   :target-dir class-dir})
    (b/compile-clj {:basis     basis
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :main      'atemoia.server
             :uber-file uber-file
             :basis     basis})))

(defn -main
  [& _]
  (build)
  (shutdown-agents))

(comment
  (build))
