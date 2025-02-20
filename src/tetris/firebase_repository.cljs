(ns tetris.firebase-repository
  (:require
    [cljs.core.async :refer [chan <! >! go-loop go timeout close!]]
    [clojure.walk :refer [keywordize-keys]]))


;; Получаем ссылку на базу данных из глобального объекта firebase
(def db (.database ^js js/firebase))
(def scores-key "scores")
(def users-key "users")


;; 1. Function to get data by key using core.async.
;; It returns a channel that emits the requested data.
(defn get-data-chan [key]
  (let [c (chan)]
    (-> (.ref db key)
        (.once "value")
        (.then (fn [snapshot]
                 (go
                   (>! c (-> snapshot .val (js->clj :keywordize-keys true)))
                   (.close snapshot))))
        (.catch (fn [err]
                  (js/console.error "Error getting data" err)
                  (go (>! c nil)))))
    c))

;; 2. Function to save data by key using core.async.
;; It returns a channel that emits the generated reference key or nil on error.
(defn save-data-chan [key data]
  (let [c (chan)]
    (-> (.ref db key)
        (.push (clj->js data))
        (.then (fn [ref]
                 (go (>! c (.-key ref)))))
        (.catch (fn [err]
                  (js/console.error "Error saving data" err)
                  (go (>! c nil)))))
    c))



;; 3. Rewrite existing functions to use the new core.async versions:

(defn get-scores []
  ;; Example of how to use get-data-chan without a callback
  (go
    (<! (get-data-chan "scores"))))

(defn save-score [score-entry]
  (go
    (<! (save-data-chan "scores" score-entry))))

(defn update-scores! [new-entry]
  (go
    (let [scores (<! (get-data-chan "scores"))
          updated (-> (concat scores [new-entry])
                      (sort-by :scores >)
                      (take 10)
                      vec)]
      (<! (save-data-chan "scores" updated))
      updated)))


;;;;;;;;;;;;;;;;;

;; Функция для сохранения результата игры
(defn save-data [{:keys [date score level username]}]
  (-> (.ref db "scores")                                    ; ссылка на раздел "scores"
      (.push (clj->js {:date date :score score :level level :user username})) ; сохраняем данные: имя, очки, время (timestamp в мс)
      (.then (fn [ref]
               (js/console.log "Результат сохранён с ключом:" (.-key ref))))
      (.catch (fn [error]
                (js/console.error "Ошибка сохранения результата:" error)))))

;; Функция для получения результатов игры
(defn get-scores [callback]
  (-> (.ref db "scores")                                    ; ссылка на раздел "scores"
      (.once "value")                                       ; один раз считываем данные (можно использовать .on для постоянного слушателя)
      (.then (fn [snapshot]
               (let [data (.val snapshot)]
                 (callback data))))
      (.catch (fn [error]
                (js/console.error "Ошибка получения результатов:" error)))))

;; Примеры вызовов:
;; (save-score "Игрок1" 1500)
;; (get-scores (fn [scores]
;;               (js/console.log "Полученные результаты:" (js->clj scores :keywordize-keys true))))

(defn update-scores!
  "Принимает новую запись new-entry (карта вида
   {:user \"name\" :date \"some date\" :scores 123 :level 3})
   и callback-функцию, в которую передаются обновлённые 10 лучших результатов."
  [new-entry callback]
  (let [scores-ref (.ref db "scores")]
    (-> (.once scores-ref "value")                          ;; читаем текущие данные
        (.then (fn [snapshot]
                 (let [data (.val snapshot)
                       ;; если данных ещё нет, создаём пустой вектор
                       existing-scores (if data
                                         (js->clj data :keywordize-keys true)
                                         [])
                       ;; добавляем новую запись
                       updated-scores (conj existing-scores new-entry)
                       ;; сортируем по убыванию поля :scores и берём первые 10
                       top-scores (->> updated-scores
                                       (sort-by :scores >)
                                       (take 2)
                                       vec)]
                   top-scores)))
        (.then (fn [top-scores]
                 ;; Перезаписываем узел "scores" новыми данными.
                 ;; (.set ...) возвращает промис, но здесь для простоты возвращаем top-scores
                 (.set scores-ref (clj->js top-scores))
                 top-scores))
        (.then (fn [top-scores]
                 (when callback
                   (callback top-scores))))
        (.catch (fn [error]
                  (js/console.error "Ошибка обновления scores:" error))))))
