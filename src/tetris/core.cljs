(ns tetris.core
  (:require [reagent.core :as r]
            [tetris.game :as g]
            [tetris.shapes :as shape]
            [tetris.field :as field]
            [tetris.scores-modal :as m]
            [tetris.user-modal :as um]
            [tetris.local-repository :as local-repo]
            [tetris.firebase-repository :as firebase-repo]
            [cljs.core.async :refer [chan <! >! go-loop go timeout close!]]
            ["react-dom/client" :refer [createRoot]]))


(def cell-size (str g/v-spacing "px"))

(defonce *state (r/atom nil))
(defonce *app-state (r/atom {:current-user (local-repo/get-user)}))

(defn key->action [key]
  (case key
    "ArrowDown" :down
    "ArrowLeft" :left
    "ArrowRight" :right
    "Shift" :rotate
    nil))

(defn keydown-handler [events]
  (fn [e]
    (when-let [action (key->action (.-key e))]
      (go (>! events action)))))

(defn calc-level-speed [state]
  (if (g/next-level? (:score state) (:level state))
    (let [new-level (g/increment-level (:level state))]
      (assoc state :level new-level :game-speed (nth g/levels-speed (dec new-level))))
    state))

(defn game-step [state action]
  (if (= :stop action)
    (assoc state :running false :game-over true)
    (let [new-state (g/update-state state action)
          new-field (field/clear-full-rows (:field new-state))]
      (-> new-state
          calc-level-speed
          (assoc :field (:field new-field))
          (update :score + (g/calc-score (:cleaned-count new-field)))))))

(defn update-scores! [new-entry]
  (if (:current-user @*app-state)
    (firebase-repo/update-scores! new-entry)
    (local-repo/update-scores! new-entry)))

(defn start-game []
  (println "Start the game")
  (let [events (chan 1)
        key-handler (keydown-handler events)
        stop-fn (fn []
                  (println "Stop the game")
                  (.removeEventListener js/window "keydown" key-handler)
                  (close! events)
                  (when (> (:score @*state) 0)
                    (go
                      (let [scores-data (<! (update-scores! {:date (.toISOString (js/Date.)) :score (:score @*state) :level (:level @*state) :user (:current-user @*app-state)}))]
                        (swap! *state assoc :score-data scores-data :show-modal true)))))]

    (.addEventListener js/window "keydown" key-handler)

    (go-loop []
             (do
               (>! events :down)
               (<! (timeout (:game-speed @*state)))
               (recur)))

    (go-loop [state (assoc (g/init) :events events)]
             (if (:running state)
               (when-let [action (<! events)]
                 (let [new-state (game-step state action)]
                   (reset! *state new-state)
                   (recur new-state)))
               (stop-fn)))
    stop-fn))

(defn game-grid []
  (let [board (r/track #(get @*state :field))
        shape-coords (r/track #(set (when (:shape @*state) (shape/get-coords (:shape @*state)))))]
    [:div {:style {:display               "grid"
                   :grid-template-columns (apply str (repeat 10 (str cell-size " ")))
                   :grid-template-rows    (apply str (repeat 20 (str cell-size " ")))
                   :background            "#c7c7c7"
                   :border                "1px solid black"
                   :box-sizing            "border-box"}}
     (doall
       (for [r (range 20) c (range 10)]
         ^{:key (str r "-" c)}
         [:div {:style {:width      cell-size
                        :height     cell-size
                        :background (cond
                                      (contains? @shape-coords [c r]) "blue"
                                      (= 1 (get-in @board [r c])) "gray"
                                      :else "white")
                        :border     "1px solid #c7c7c7"
                        :box-sizing "border-box"}}]))]))

(defn down-coords [coords]
  (map (fn [[x y]] [x (inc y)]) coords))

(defn next-piece-grid []
  (let [shape-coords (r/track #(set (when (:shape @*state) (down-coords (shape/get-coords (:next-shape @*state))))))]
    [:div {:style {:display               "grid"
                   :grid-template-columns (apply str (repeat 4 (str cell-size " ")))
                   :grid-template-rows    (apply str (repeat 4 (str cell-size " ")))
                   :background            "black"
                   :margin-left           cell-size}}
     (doall
       (for [r (range 4) c (range 4)]
         ^{:key (str r "-" c)}
         [:div {:style {:width      cell-size
                        :height     cell-size
                        :background (if (contains? @shape-coords [c r]) "blue" "white")}}]))]))


(defn score-view []
  (let [score @(r/track #(:score @*state))]
    [:div "Score: " score]))

(defn level-view []
  (let [score @(r/track #(:level @*state))]
    [:div "Level: " score]))

(defn user-name-component []
  [:div#username "User name"])

;; Update game-component to use game state for modal
(defn game-component []
  (let [stop-fn (r/atom nil)]
    (fn []
      [:div.container {:style {:display         "flex"
                               :flex-direction  "column"
                               :justify-content "center"
                               :align-items     "center"}}
       [:div {:style {:display "flex" :padding cell-size :gap "20px"}}
        [game-grid]
        [:div {:style {:display        "flex"
                       :flex-direction "column"
                       :align-items    "center"
                       :gap            "10px"}}
         [next-piece-grid]
         [um/user-name-component *app-state]
         [level-view]
         [score-view]
         [:button.button {:id       "start-btn"
                          :on-click (fn [e]
                                      (reset! stop-fn (start-game))
                                      (.blur (.-target e)))} "Start"]
         [:button.button {:id       "stop-btn"
                          :on-click #(when @stop-fn (@stop-fn))} "Stop"]
         [:div {:id    "game-over"
                :style {:color      "red"
                        :font-size  "18"
                        :margin-top "10px"
                        :display    (if (:game-over @*state) "block" "none")}}
          "Game over!"]]
        [m/modal (:show-modal @*state) (:score-data @*state)]]])))


(defonce root (createRoot (.getElementById js/document "app")))

(defn init
  []
  (.render root (r/as-element [game-component])))

(defn ^:dev/after-load re-render
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (init))

(comment
  (shadow/watch :app)
  (shadow/repl :app))