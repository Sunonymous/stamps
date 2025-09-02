(ns stamps.events
  (:require
   [re-frame.core :as re-frame]
   [stamps.db :refer [default-db localStorage-key]]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [cljs.reader :refer [read-string]]
   [stamps.data :refer [next-id new-log]]
   ))

(defn db->local-storage
  [db]
  (js/localStorage.setItem localStorage-key (str db)))

(re-frame/reg-cofx
 :localStorage
 (fn [coeffects storage-key]
   (assoc coeffects :localStorage
          (js/localStorage.getItem storage-key))))

(def ->localStorage (re-frame/after db->local-storage))

(re-frame/reg-event-fx
 ::initialize-db
 [(re-frame/inject-cofx :localStorage localStorage-key)]
 (fn-traced [cofx _]
            (let [persisted-data (read-string (:localStorage cofx))]
              (if persisted-data
                {:db (merge default-db persisted-data)}
                {:db default-db}))))


(re-frame/reg-event-db
 ::new-log
 [->localStorage]
 (fn-traced [db [_ new-name due-on goal-code]]
            (let [id (next-id (:logs/by-id db))]
              (assoc-in db [:logs/by-id id] (cond-> (new-log id new-name)
                                              due-on
                                              (assoc :due-on due-on)
                                              goal-code
                                              (assoc :goal goal-code)) )))
 )

(comment
  (let [due-on    nil
        goal-code "3:1:week"]
    (cond-> (new-log 3 "jump")
      due-on
      (assoc :due-on 1750721425139)
      goal-code
      (assoc :goal-code goal-code)))
  :rcf
  )

(re-frame/reg-event-db
 ::target-log
 (fn-traced [db [_ id]]
           (assoc db :target-log id)))

(re-frame/reg-event-db
 ::stamp-log
 [->localStorage]
 (fn-traced [db [_ id]]
            (update-in db [:logs/by-id id :timestamps]
                       #(cons (.getTime (js/Date.)) %))))

(re-frame/reg-event-db
 ::delete-log
 (fn-traced [db [_ id]]
   (update-in db [:logs/by-id] dissoc id)))

(re-frame/reg-event-db
 ::schedule-for-deletion
 [->localStorage]
 (fn-traced [db [_ id]]
   (update-in db [:ledgers :recently-deleted] conj id)))

(re-frame/reg-event-db
 ::rename-log
 [->localStorage]
 (fn-traced [db [_ id new-name]]
   (assoc-in db [:logs/by-id id :name] new-name)))

(re-frame/reg-event-db
 ::set-active-ledger
 (fn-traced [db [_ ledger]]
   (assoc db :active-ledger (keyword ledger))))

(re-frame/reg-event-db
 ::remove-timestamp-from-log
 (fn-traced [db [_ log-id timestamp]]
   (update-in db [:logs/by-id log-id :timestamps] #(filter (partial not= timestamp) %))))

(re-frame/reg-event-db
 ::toggle-reverse-sort
 (fn-traced [db _]
            (update db :reverse-sort not)))

(re-frame/reg-event-db
 ::set-sort-parameter
 (fn-traced [db [_ next-sort-param]]
            (assoc db :sort-parameter (keyword next-sort-param))))