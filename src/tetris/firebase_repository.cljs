(ns tetris.firebase-repository
  (:require
    [cljs.core.async :refer [chan <! >! go-loop go timeout close! put!]]
    [clojure.walk :refer [keywordize-keys]]))


;; Получаем ссылку на базу данных из глобального объекта firebase
(def db (.database ^js js/firebase))
(def scores-key "scores")
(def users-key "users")


;; 1. Function to get data by key using core.async.
;; It returns a channel that emits the requested data.
(defn get-data-chan [key]
  (let [c (chan)]
    (.. db
        (ref)
        (child key)
        (get)
        (then (fn [snapshot]
                (if (.exists snapshot)
                  (go
                    (>! c (-> snapshot .val (js->clj :keywordize-keys true)))
                    (close! c))
                  (go
                    (js/console.log "No data available")
                    (close! c)))))
        (catch (fn [error]
                 (js/console.error error)
                 (go (close! c)))))
    c))

;; 2. Function to save data by key using core.async.
;; It returns a channel that emits the generated reference key or nil on error.
(defn save-data [key data]
  (-> (.ref db key)
      (.set (clj->js data))
      (.catch (fn [err]
                (js/console.error "Error saving data" err)))))



;; 3. Rewrite existing functions to use the new core.async versions:

(defn get-scores []
  ;; Example of how to use get-data-chan without a callback
  (go
    (<! (get-data-chan "scores/"))))

(defn save-score [score-entry]
  (go
    (<! (save-data "scores/" score-entry))))

(defn update-scores! [new-entry]
  (go
    (let [scores (<! (get-data-chan "scores/"))
          updated (->> (concat scores [new-entry])
                      (sort-by :scores >)
                      (take 10)
                      vec)]
      (save-data "scores/" updated)
      updated)))
