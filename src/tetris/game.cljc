(ns tetris.game
  (:require [tetris.shapes :as s]
            [tetris.field :as field]))


(def width 300)
(def height 600)

(def v-count 10)
(def h-count 20)

(def v-spacing (/ width v-count))

(defn get-coords [state]
  (s/get-coords (:shape state)))

(def levels-speed
  [400 350 300 250 200 150 100])

(def scores-threshold 200)

(defn next-level? [score current-level]
  (<= scores-threshold (/ score current-level)))

(defn init []
  {:shape      (s/get-random-shape)
   :field      (field/create-field v-count h-count)
   :game-over  false
   :running    true
   :next-shape (s/get-random-shape)
   :actions    #{}
   :level 1
   :game-speed (first levels-speed)})

(defn out-of-bounds? [field [x y]]
  (let [rows (count field)
        cols (count (first field))]
    (or (< x 0) (>= x cols) (>= y rows))))

(defn intersects? [field coords]
  (some (fn [[x y]]
          (or (out-of-bounds? field [x y])
              (= 1 (get-in field [y x]))))
        coords))



(defn apply-action [state action]
  (case action
    :rotate (update-in state [:shape] s/rotate-shape)
    ;; :up :down :left :right
    (update-in state [:shape] s/move-shape action)))

(defn bake [state]
  (println "Bake")
  (let [new-state (update-in state [:field] field/bake (s/get-coords (:shape state)))]
    (assoc new-state
      :shape (:next-shape state)
      :next-shape (s/get-random-shape))))

(defn game-over? [field]
  (some (fn [v] (= 1 v)) (first field)))

(defn stop-game [state]
  (assoc state :game-over true :running false))

(defn current-shape [state]
  (s/get-coords (:shape state)))

(defn next-shape [state]
  (map (fn [[x y]] [x (inc y)]) (s/get-coords (:next-shape state))))

(defn update-state [state action]
  (let [next-state (apply-action state action)
        next-coords (get-coords next-state)
        field (:field state)]
    (if (game-over? field)
      (stop-game state)

      (case action
        :down
        (cond
          (and (intersects? field next-coords) (some (fn [[_ y]] (= 0 y)) next-coords))
          (stop-game state)
          (intersects? field next-coords)
          (bake state)
          :else next-state)

        :left
        (if
          (intersects? field next-coords)
          state
          next-state)

        :right
        (if
          (intersects? field next-coords)
          state
          next-state)

        :rotate
        (if
          (intersects? field next-coords)
          (let [shifted-right (apply-action next-state :right)
                shifted-left (apply-action next-state :left)]
            (cond
              (and (intersects? field (get-coords shifted-left)) (intersects? field (get-coords shifted-right)))
              state
              (intersects? field (get-coords shifted-left))
              shifted-right
              :else shifted-left))
          next-state)))))

(defn calc-score [cleared-lines]
  (let [coef (if (> cleared-lines 1) (* cleared-lines 1.5) cleared-lines)]
    (* coef v-count)))

(defn increment-level [n]
  (if (< n (count levels-speed))
    (inc n)
    n))