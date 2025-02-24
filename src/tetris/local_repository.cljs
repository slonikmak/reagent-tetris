(ns tetris.local-repository
  (:require [cljs.core.async :refer [go chan >! <!]]))

(def repo-key "tetris-scores")
(def user-key "tetris-user")

(defn get-scores []
  (go
    (let [data (js/localStorage.getItem repo-key)]
      (if data
        (js->clj (js/JSON.parse data) :keywordize-keys true)
        []))))

(defn save-data [data]
  (js/localStorage.setItem repo-key (js/JSON.stringify (clj->js data))))

(defn update-scores! [new-entry]
  (println "Update local repo")
  (go
    (let [data (or (<! (get-scores)) [])
          updated-data (->> (conj data new-entry)
                            (sort-by :score >)
                            (take 10)
                            vec)]
      (save-data updated-data)
      [new-entry updated-data])))

(defn save-user [username]
  (js/localStorage.setItem user-key (clj->js username)))

(defn get-user []
  (let [user (js/localStorage.getItem user-key)]
    (when (not= user "null") (js->clj user))))

