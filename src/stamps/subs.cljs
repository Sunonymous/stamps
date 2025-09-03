(ns stamps.subs
  (:require
   [re-frame.core :as re-frame]
   [stamps.util :as util]))

(re-frame/reg-sub
 ::logs-all
 (fn [db]
   (vals (:logs/by-id db))))

(re-frame/reg-sub
 ::log
 (fn [db [_ id]]
   (get-in db [:logs/by-id id])))

(re-frame/reg-sub
 ::logs-by-id
 (fn [db [_ ids]]
   (map #(get-in db [:logs/by-id %]) ids)))

(re-frame/reg-sub
 ::sorted-logs-by-id
 (fn [db [_ ids sort-fn]]
   (sort-by sort-fn (map #(get-in db [:logs/by-id %]) ids))))

(re-frame/reg-sub
 ::logs-with-parameter-from-ids
 (fn [db [_ ids key]]
   (map (fn [id]
          [id (get-in db [:logs/by-id id key])])
        ids)))

(re-frame/reg-sub
 ::logs-with-parameter
 (fn [db [_ key]]
   (map (fn [[id log]] [id (log key)])
        (db :logs/by-id))
   ))

(re-frame/reg-sub
 ::log-stamps
 (fn [db [_ id]]
   (get-in db [:logs/by-id id :timestamps])))

(re-frame/reg-sub
 ::target-log
 (fn [db]
   (:target-log db)))

(re-frame/reg-sub
 ::active-ledger
 (fn [db]
   (:active-ledger db)))

(re-frame/reg-sub
 ::log-ids-all
 (fn [db _]
   (keys (:logs/by-id db))))

(re-frame/reg-sub
 ::log-ids-needs-attention
 :<- [::logs-all]
 (fn [logs _]
   (util/logs->filtered-ids logs [util/due-today?
                                  util/has-future-due-date-and-no-stamps?
                                  util/has-unmet-goal?
                                 ])))

(re-frame/reg-sub
 ::log-ids-pred-testing
 :<- [::logs-all]
 (fn [logs _]
   (util/logs->filtered-ids logs [
                                  util/has-future-due-date?
                                 ]))
 )

(re-frame/reg-sub
 ::log-ids-archived
 :<- [::logs-all]
 (fn [logs _]
   (util/logs->filtered-ids logs [(partial util/should-be-archived? (util/ms-in-minutes 5))]))) ;; TODO this should depend on config subscription, not hardcoded values

(re-frame/reg-sub
 ::ordered-ids
 (fn [db]
   ))

(re-frame/reg-sub
 ::reverse-sort
 (fn [db]
   (:reverse-sort db)))

(re-frame/reg-sub
 ::sort-parameter
 (fn [db]
   (:sort-parameter db)))