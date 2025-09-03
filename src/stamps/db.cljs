(ns stamps.db
  (:require
   [stamps.util :as util]))

(def localStorage-key "stamps-db")

(def default-db
  {:logs/by-id     {}
   :history        [] ;; history of logs interacted with. resets daily
   :config         {:archive-threshold (util/ms-in-days 15) ;; logs without a timestamp in this amount of time are considered archived
                    :goals-first?      false ;; goals shown before deadlines in needs-attention
                    :delete-after      (util/ms-in-days 7) ;; if logs are in the deleted ledger, they will be removed after 7 days
                    }
   :active-ledger :all ;; all/needs-attention ;; TODO add more ledgers!
   :sort-parameter :id
   :reverse-sort   false
   #_:ledgers    #_{:needs-attention    [] ;; has goal/deadline
                    :overdue            [] ;; has deadline
                    :uninterested       [] ;; user has continually passes
                    :recently-created   [] ;; making a new log sends its ID this way
                    :recently-updated   [] ;; saving an edit adds the log ID here
                    :recently-deleted   [] ;; marking a log for deletion sends its ID here
                    :recently-completed [] ;; added to this list upon completion of a log
                    :archived           [] ;; logs with inaction longer than the archive threshold
                    }})
