(ns tetris.scores-modal)

(defn format-date [date-str]
  (.toLocaleString (js/Date. date-str)))

(defn modal [show? [new-scores scores-list]]
  (when show?
    [:div {:style {:position "fixed"
                   :top "0"
                   :left "0"
                   :width "100%"
                   :height "100%"
                   :background-color "rgba(0,0,0,0.5)"
                   :display "flex"
                   :justify-content "center"
                   :align-items "center"}}
     [:div {:style {:background-color "white"
                    :padding "20px"
                    :border-radius "5px"
                    :min-width "300px"
                    :box-shadow "0 2px 10px rgba(0,0,0,0.1)"}}
      [:h2 "High Scores"]
      [:table {:style {:width "100%" :border-collapse "collapse" :margin-bottom "15px"}}
       [:thead
        [:tr
         (when (:user new-scores) [:th {:style {:padding "8px" :border "1px solid #ddd"}} "User"])
         [:th {:style {:padding "8px" :border "1px solid #ddd"}} "Date"]
         [:th {:style {:padding "8px" :border "1px solid #ddd"}} "Score"]
         [:th {:style {:padding "8px" :border "1px solid #ddd"}} "Level"]]]
       [:tbody
        (for [{:keys [user date score level]} scores-list]
          ^{:key date}
          [:tr {:style {:background-color (when (= date (:data new-scores)) "#e6ffe6")}}
           (when (:user new-scores) [:td {:style {:padding "8px" :border "1px solid #ddd"}} user])
           [:td {:style {:padding "8px" :border "1px solid #ddd"}} (format-date date)]
           [:td {:style {:padding "8px" :border "1px solid #ddd"}} score]
           [:td {:style {:padding "8px" :border "1px solid #ddd"}} level]])]]
      [:div {:style {:display "flex" :justify-content "center"}}
       [:button {:on-click #(js/location.reload)} "New Game"]]]])
  )