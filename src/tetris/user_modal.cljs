(ns tetris.user-modal
  (:require [reagent.core :as r]
            [cljs.core.async :refer [go <!]]
            [tetris.firebase-repository :as repo]
            [tetris.local-repository :as local-repo]))

(defn- save-user [input-name validation-message *app-state show-modal]
  (go
    (let [user-name (if (= @input-name "") nil @input-name)
          users (<! (repo/get-users))]
      (if (some #(= user-name %) users)
        (reset! validation-message "Пользователь с таким именем уже существует!")
        (do
          (swap! *app-state assoc :current-user user-name)
          (repo/update-users! user-name)
          (local-repo/save-user user-name)
          (reset! show-modal false))))))

(defn atom-input [value]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))
           :placeholder "Введите имя пользователя"}])

(defn user-name-component [*app-state]
  (let [show-modal (r/atom false)
        input-name (r/atom (:current-user @*app-state))
        validation-message (r/atom nil)]
    (fn []
      [:div#username
       ;; Отображаем текущее имя пользователя или сообщение, если его нет
       (let [current @input-name]
         [:div
          (if current
            [:span current]
            [:span "Anonymous"])
          [:button.button.is-small {:on-click #(do
                                 (reset! input-name current)
                                 (reset! validation-message nil)
                                 (reset! show-modal true))}
           "✏️"]])

       ;; Модалка редактирования
       (when @show-modal
         [:div {:style {:position         "fixed"
                        :top              "0"
                        :left             "0"
                        :width            "100%"
                        :height           "100%"
                        :background-color "rgba(0,0,0,0.5)"
                        :display          "flex"
                        :justify-content  "center"
                        :align-items      "center"}}
          [:div {:style {:background-color "white"
                         :padding          "20px"
                         :border-radius    "5px"
                         :min-width        "300px"
                         :box-shadow       "0 2px 10px rgba(0,0,0,0.1)"}}
           [:h2 "Редактирование имени"]
           [atom-input input-name]
           (when @validation-message
             [:div {:style {:color "red" :margin-top "10px"}}
              @validation-message])
           [:div {:style {:display         "flex"
                          :justify-content "space-between"
                          :margin-top      "20px"}}
            [:button {:on-click #(reset! show-modal false)}
             "Отмена"]
            [:button {:on-click #(save-user input-name validation-message *app-state show-modal)}
             "Сохранить"]]]])])))
