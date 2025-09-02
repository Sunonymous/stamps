(ns stamps.data)

(def constants
  {:master-id 1})

;; Here lives the law of the data

;; Logs look like this
;; {:id 1 :name "Walk the Plank" :steps [] :timestamps [] }
;
;; Logs can have a deadline.
;; {:id 1 :name "Walk the Plank" :steps [] :timestamps [] :due-on 1750721425139 }
;; Deadlines are the number of milliseconds since the epoch
;; Logs which pass their deadlines are not shown in the default view.
;; Timestamps are the number of milliseconds since the epoch

;; Logs can also have a goal.
;; {:id 1 :name "Walk the Plank" :steps [] :timestamps [] :goal-span :weekly :goal-count 1 }
;; Goal-span is the amount of time the user wants to complete this log.
;; Goal-count is the number of stamps needed to be considered complete.
;;  Goal-count may be omitted if a single stamp is needed.

;; TODO Logs can have steps. Eventually. But not right now.

(defn next-id
  "Given a map of {id log}, returns the proper integer ID
   value for a newly created log."
  [logs]
  (if (seq logs)
    (->> logs
         keys
         (apply max)
         inc)
    (constants :master-id)))

(defn new-log [id name]
    {:id         id
     :name       name
     :timestamps []})

(defn make-due-on [log ms]
  (assoc log :due-on ms))

(defn base-filter
  "Basic filter over logs that run the program. (Hopefully) Includes:
   - has been stamped within archive point
   - if deadline is present, it is in the future"
  [log]
  (and
   #() ; TODO should-be-archived?
   #() ; TODO future-dated deadline?
   #() ; TODO goal still incomplete?
   ))

; Log Predicate Functions

(defn has-goal? [log]
  (contains? log :goal)) ;; TODO test for goal validity? or just assume it?

(defn has-deadline? [log]
  (contains? log :due-on))

(defn has-stamps? [log]
  (if (seq (log :timestamps))
    true false)) ;; not sure if this helps; didn't want nil results in testing

(defn should-be-archived? [archive-threshold log]
  (let [adjusted-time (.getTime (js/Date. (- (js/Date.now) archive-threshold)))
        _ (js/console.log "adjusted-time: " adjusted-time)]
    (and (has-stamps? log)
         ;; first timestamp is older than archive threshold
         (< (first (log :timestamps)) adjusted-time))))

(comment
  (should-be-archived? (util/ms-in-minutes 5) {:id 1 :name "Walk the Plank"  :timestamps [1750721425139] :due-on 1750721425139})
  (should-be-archived? (util/ms-in-minutes 1) {:id 1 :name "Walk the Plank"  :timestamps [] :due-on 1750721425139})

  :rcf)