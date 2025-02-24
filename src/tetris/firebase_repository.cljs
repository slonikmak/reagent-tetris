(ns tetris.firebase-repository
  (:require
    [cljs.core.async :refer [chan <! >! go-loop go timeout close! put!]]))


;; Получаем ссылку на базу данных из глобального объекта firebase
(def db (.database ^js js/firebase))
(def scores-key "scores/")
(def users-key "users/")


;; 1. Function to get data by key using core.async.
;; It returns a channel that emits the requested data.
(defn get-data-chan [key]
  (let [c (chan)]
    (.. db
        (ref)
        (child key)
        (get)
        (then (fn [snapshot]
                (go
                  (if (.exists snapshot)
                    (>! c (-> snapshot .val (js->clj :keywordize-keys true)))
                    (do
                      (js/console.log "No data available")
                      (>! c [])))
                  (close! c))))
        (catch (fn [error]
                 (js/console.error error)
                 (go (close! c)))))
    c))

;; 2. Function to save data by key using core.async.
;; It returns a channel that emits the generated reference key or nil on error.
(defn update-data [key data]
  (-> (.ref db key)
      (.set (clj->js data))
      (.catch (fn [err]
                (js/console.error "Error saving data" err)))))



;; 3. Rewrite existing functions to use the new core.async versions:

(defn get-scores []
  (get-data-chan scores-key))

(defn save-score [score-entry]
  (go
    (<! (update-data scores-key score-entry))))

(defn update-scores! [new-entry]
  (println "Update firebase")
  (go
    (let [scores (<! (get-scores))
          updated (->> (concat scores [new-entry])
                       (sort-by :score >)
                       (take 10)
                       vec)]
      (update-data scores-key updated)
      [new-entry updated])))

(comment
  (go (println (<! (get-scores))))

  (go (println (<! (update-scores! {:user "dd" :date (js/Date.now) :level 1 :score 21})))))

(defn get-users []
  (get-data-chan users-key))

(defn update-users! [new-username]
  (go
    (let [users (<! (get-users))
          updated (vec (concat users [new-username]))]
      (if new-username
        (do
          (update-data users-key updated)
          updated)
        users))))
