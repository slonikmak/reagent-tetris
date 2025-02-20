(ns tetris.local-repository)

(def repo-key "tetris-scores")

(defn get-scores []
  (let [data (js/localStorage.getItem repo-key)]
    (if data
      (js->clj (js/JSON.parse data) :keywordize-keys true)
      [])))

(defn save-data [data]
  (js/localStorage.setItem repo-key (js/JSON.stringify (clj->js data))))

(defn save-scores [{:keys [date score level]}]
  (let [data (or (-> (get-scores)
                     js/JSON.parse
                     js->clj
                     vec) [])
        new-entry {:date date :score score :level level}
        updated-data (->> (conj data new-entry)
                          (sort-by :score >)
                          (take 10))]
    (save-data updated-data)
    [new-entry updated-data]))

