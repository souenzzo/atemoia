(ns atemoia.note
  (:require [clojure.string :as string]))

(defn valid?
  [x]
  (and (string? x)
    (not (string/blank? x))))
