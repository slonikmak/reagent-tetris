(ns tetris.shapes)

(def tetromino-rotation-states
  {:I [#{[0 0] [1 0] [2 0] [3 0]}                           ; Horizontal
       #{[1 -1] [1 0] [1 1] [1 2]}                          ; Vertical
       #{[0 1] [1 1] [2 1] [3 1]}                           ; Horizontal (flipped)
       #{[2 -1] [2 0] [2 1] [2 2]}]                         ; Vertical (flipped)}

   :O [#{[0 0] [0 1] [1 0] [1 1]}                           ; Square (no rotation)
       #{[0 0] [0 1] [1 0] [1 1]}
       #{[0 0] [0 1] [1 0] [1 1]}
       #{[0 0] [0 1] [1 0] [1 1]}]

   :T [#{[0 0] [1 0] [2 0] [1 1]}                           ; T pointing up
       #{[1 -1] [1 0] [1 1] [2 0]}                          ; T pointing right
       #{[0 1] [1 1] [2 1] [1 0]}                           ; T pointing down
       #{[0 0] [1 -1] [1 0] [1 1]}]                         ; T pointing left

   :S [#{[1 0] [2 0] [0 1] [1 1]}                           ; S horizontal
       #{[1 -1] [1 0] [2 0] [2 1]}                          ; S vertical
       #{[1 1] [2 1] [0 0] [1 0]}                           ; S horizontal (flipped)
       #{[0 -1] [0 0] [1 0] [1 1]}]                         ; S vertical (flipped)

   :Z [#{[0 0] [1 0] [1 1] [2 1]}                           ; Z horizontal
       #{[2 -1] [1 0] [2 0] [1 1]}                          ; Z vertical
       #{[0 1] [1 1] [1 0] [2 0]}                           ; Z horizontal (flipped)
       #{[0 0] [1 -1] [0 1] [1 0]}]                         ; Z vertical (flipped)

   :J [#{[0 0] [1 0] [2 0] [0 1]}                           ; J pointing up
       #{[1 -1] [1 0] [1 1] [2 1]}                          ; J pointing right
       #{[0 1] [1 1] [2 1] [2 0]}                           ; J pointing down
       #{[0 -1] [1 -1] [1 0] [1 1]}]                        ; J pointing left

   :L [#{[0 0] [1 0] [2 0] [2 1]}                           ; L pointing up
       #{[1 -1] [1 0] [1 1] [0 1]}                          ; L pointing right
       #{[0 1] [1 1] [2 1] [0 0]}                           ; L pointing down
       #{[2 -1] [1 -1] [1 0] [1 1]}]                        ; L pointing left
   })

(comment
  ;; Shape is:
  {:coords        [#{[0 0] [1 0] [2 0] [2 1]}               ; L pointing up
                   #{[1 -1] [1 0] [1 1] [0 1]}              ; L pointing right
                   #{[0 1] [1 1] [2 1] [0 0]}               ; L pointing down
                   #{[2 -1] [1 -1] [1 0] [1 1]}]
   :current-state 1}
  )

(defn get-coords [shape]
  (nth (:coords shape) (:current-state shape)))


(defn rotate-shape [shape]
  (let [new-state (mod (inc (:current-state shape)) 4)]
    (assoc shape :current-state new-state)))

(defn shift-shape [shape dx dy]
  (let [shift-coords (fn [coords]
                       (set (map (fn [[x y]] [(+ x dx) (+ y dy)]) coords)))
        updated-coords (mapv shift-coords (:coords shape))]
    (assoc shape :coords updated-coords)))

(defn move-shape [shape direction]
  (let [[dx dy] (case direction
                  :left [-1 0]
                  :right [1 0]
                  :up [0 -1]
                  :down [0 1])]
    (shift-shape shape dx dy)))


(defn get-random-shape []
  (let [shapes (keys tetromino-rotation-states)
        random-shape ((rand-nth shapes) tetromino-rotation-states)]
    {:coords random-shape
     :current-state (rand-int 4)}))

(comment
  (get-random-shape))