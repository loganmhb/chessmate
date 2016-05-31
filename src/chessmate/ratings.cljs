(ns chessmate.ratings)

;;; Move rating system adapted from this paper:
;;; http://www.aaai.org/ocs/index.php/AAAI/AAAI11/paper/view/3779/3962

(defn weight
  "Applies a simple weighting function to the raw diffference in
  evaluation between the selected move and the optimal move, to
  reflect the decreasing importance of marginal increases in the
  difference in evaluation. (A five-pawn blunder is probably not more
  than twice as bad as a five-pawn blunder, since they are both likely
  to be fatal for a strong player.)"
  [delta]
  (Math/log (inc delta)))

(defn delta->proxy
  "Convert an evaluation differential into a probability proxy based
  on agent params."
  [{:keys [s c] :as agent} delta]
  (Math/exp (- (Math/pow (/ (weight delta) s) c))))

(defn delta [bestmove move]
  (- (:evaluation bestmove) (:evaluation move)))

(defn proxies->probabilities
  "The proxies can be converted to probabilities by satisfying the following
  constraints:
    1. the sum of all probabilities is one (since a move must be chosen)
    2. the probabilities are related to the proxies by some consistent function

  This is a simple normalization. TODO: implement the more effective one
  described in the paper."
  [moves-with-proxies]
  (let [sum-proxies (apply + (map :proxy moves-with-proxies))]
    (map (fn [{:keys [proxy] :as move}]
           (assoc move :probability (/ proxy sum-proxies)))
         moves-with-proxies)))


(defn calculate-proxies [agent possible-moves]
  (let [e0 (:evaluation (first possible-moves))
        deltas (map (fn [move] (- e0 (:evaluation move)))
                    possible-moves)
        weighted-deltas (map weight deltas)
        proxies (map (partial delta->proxy agent) weighted-deltas)]
    (map (fn [move proxy] (assoc move :proxy proxy)) possible-moves proxies)))


(defn ranges [probabilities]
  (partition 2 1 (reductions + 0 probabilities)))

(defn pick-move-for-agent
  "Pick a plausible random move for an agent. "
  [agent evaluations]
  (let [sorted-evaluations (reverse (sort-by :evaluation evaluations))
        moves-with-probability (proxies->probabilities
                                (calculate-proxies agent sorted-evaluations))
        rand-ranges (ranges (map :probability moves-with-probability))
        moves-with-ranges (map (fn [move p]
                                 (assoc move :probability-range p))
                               sorted-evaluations
                               rand-ranges)]
    (reduce (fn [random-number {:keys [probability-range] :as move}]
              (if (< (first probability-range)
                     random-number
                     (second probability-range))
                (reduced (:move move))
                random-number))
            (rand)
            moves-with-ranges)))
