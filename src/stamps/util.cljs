(ns stamps.util
  (:require [clojure.string :as str]))

; Time bits
(defn ms-in-seconds [s] (* s 1000))
(defn ms-in-minutes [m] (* m 60 1000))
(defn ms-in-hours   [h] (* h 60 60 1000))
(defn ms-in-days    [d] (* d 24 60 60 1000))
(defn ms-in-weeks   [w] (* w 7 24 60 60 1000))
(defn ms-in-years   [y] (* y 365 24 60 60 1000))

(def idx->weekday
  {0 "Sunday"
   1 "Monday"
   2 "Tuesday"
   3 "Wednesday"
   4 "Thursday"
   5 "Friday"
   6 "Saturday"})

; General

(defn clamp [min max x]
    (js/Math.max min (js/Math.min max x)))

; Logs

; So I seem to have encoded the goals as a string of "<number-of-stamps>:<number-of-time-units>:<time-unit>"
;  where time-unit is one of day or week

; We'll need to reference these somewhere.

(def timespan->ms {"day"   (ms-in-days 1)
                   "week"  (ms-in-weeks 1)})

(defn ms->timelength-string
  "Converts a number of milliseconds into a human-readable string. Will return nil if time is zero."
  [ms]
  (let [units [{:unit "year"   :ms (ms-in-years   1)}
               {:unit "week"   :ms (ms-in-weeks   1)}
               {:unit "day"    :ms (ms-in-days    1)}
               {:unit "hour"   :ms (ms-in-hours   1)}
               {:unit "minute" :ms (ms-in-minutes 1)}
               {:unit "second" :ms (ms-in-seconds 1)}]
        result (reduce (fn [acc unit]
                         (let [number (js/Math.floor (/ (:remaining-ms acc) (:ms unit)))]
                           (if (pos? number)
                             (assoc acc :remaining-ms (- (:remaining-ms acc) (* number (:ms unit)))
                                    :parts (conj (:parts acc) (str number " " (:unit unit) (when (> number 1) "s"))))
                             acc)))
                       {:remaining-ms ms :parts []}
                       units)
        parts (:parts result)]
    (when (seq parts)
      (str/join ", " parts))))

(defn log->due-date-string
  [log]
  (let [due-on (log :due-on)]
    (when due-on
      (.toDateString (js/Date. due-on)))))

(defn date-string->local-ms
  [date]
  (let [[y m d]    (str/split date #"-")
        local-date (js/Date. (js/parseInt y) (dec (js/parseInt m)) (js/parseInt d))]
    (.getTime local-date)))

(defn due-today?
  "Returns true if the log is due today. The due date for a log may be set to
   the value of a date type input, or, as the value of the
   weekday index of a JS Date object."
  [log]
  (let [today (js/Date.)
        deadline (js/Date. (log :due-on))]
    (if (< (log :due-on) 7)
      (= (.getDay today) (js/parseInt (log :due-on)))
      (and (= (.getFullYear today) (.getFullYear deadline))
           (= (.getMonth today)    (.getMonth deadline))
           (= (.getDate today)     (.getDate deadline))))))

(comment

  (ms->timelength-string 1000000)
  (ms->timelength-string 0000)
  (ms->timelength-string 99999990)

  (js/console.log (js/Date.now))


  (js/Date. 1751426972341)

  (let [sample-log {:id 1, :name "Hi there", :timestamps
                    ["2025-07-01T21:11:11.999Z" "2025-07-01T21:11:35.091Z"]
                    :goal "1:2:day"}
        [num-stamps num-time-units time-unit] (clojure.string/split (sample-log :goal) #":")
        goal-window-ms (* num-time-units (timespan->ms time-unit))
        now            (js/Date.now)
        goal-cutoff    (js/Date. (- now goal-window-ms))]
    ;; (.toISOString (js/Date. two-days-ago))
    (js/console.log "goal-cutoff: " goal-cutoff)
    (and (>= (count (:timestamps sample-log)) num-stamps)
         (filter #(>= % goal-cutoff) (:timestamps sample-log))) ;; TODO test!
    )
  :rcf)


;; New topic: ledgers ;;

(defn logs->filtered-ids
  [logs preds]
  (loop [remaining  logs
         predicates preds
         acc        []  ]
    (if (empty? predicates)
      acc
      (let [pred           (first predicates)
            matches        (filter pred remaining)
            matched-ids    (map :id matches)
            next-remaining (remove (set matches) remaining)]
        (recur next-remaining (rest predicates) (into acc matched-ids))))))

(defn has-stamps? [log]
  (if (seq (log :timestamps))
    true false)) ;; not sure if this helps; didn't want nil results in testing

(defn has-goal? [log]
  (contains? log :goal)) ;; TODO test for goal validity? or just assume it?

(defn has-deadline? [log]
  (contains? log :due-on))

; just a reminder that :due-on, when it is 0-6, represents a day of the week
; otherwise it is the moment in ms of a particular calendar day

(defn has-calendar-due-date? [log]
  (and
   (contains? log :due-on)
   (< 6 (log :due-on))))

(defn due-weekly? [log]
  (and
   (contains? log :due-on)
   (> 7 (log :due-on))))

(defn has-future-due-date? [log]
  (and
   (contains? log :due-on)
   (< (js/Date.now) (log :due-on))))

(defn has-future-due-date-and-no-stamps? [log]
  (and
   (has-future-due-date? log)
   (not (has-stamps? log))))

(defn has-unmet-goal? [log]
  (let [[stamps-needed num-time-units time-unit] (str/split (:goal log) #":")
        goal-window-ms (* num-time-units (timespan->ms time-unit))
        now            (js/Date.now)
        goal-cutoff    (js/Date. (- now goal-window-ms))
        stamps-in-goal-window (count (filter #(>= % goal-cutoff) (:timestamps log)))]
    (< stamps-in-goal-window stamps-needed)))

(defn should-be-archived? [archive-threshold log]
  (let [now (js/Date.now) ;; TODO this function is yet to work. I think the logic may be confused
        adjusted-time (- now archive-threshold)
        _ (js/console.log "adjusted-time: " (.toISOString (js/Date. adjusted-time)))]
    (and (has-stamps? log)
         ;; first timestamp is older than archive threshold
         (< (first (log :timestamps)) adjusted-time))))

(comment
  (let [logs (vals {1 {:id 1, :name "Don't Get Attached..", :timestamps [], :due-on 1752127200000}, 2 {:id 2, :name "Erry Week", :timestamps [1752124089418], :due-on "3"}, 3 {:id 3, :name "2day?", :timestamps [], :due-on "3"}, 4 {:id 4, :name "2morrow?", :timestamps [], :due-on "4"}, 5 {:id 5, :name "test", :timestamps []}, 6 {:id 6, :name "sss", :timestamps [], :due-on 1752559200000}, 7 {:id 7, :name "Trigger", :timestamps []}})]
    (loop [remaining  logs
           predicates [#(odd? (:id %))]
           acc        []]
      (if (empty? predicates)
        acc
        (let [pred           (first predicates)
              matches        (filter pred remaining)
              matched-ids    (map :id matches)
              next-remaining (remove (set matches) remaining)]
          (recur next-remaining (rest predicates) (into acc matched-ids))))))



  :rcf)