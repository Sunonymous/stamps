(ns stamps.views
  (:require
   [reagent.core   :as r]
   [re-frame.core  :as rf]
   [clojure.string :as str]
   [stamps.subs    :as subs]
   [stamps.events  :as events]
   [stamps.util   :refer [clamp]]
   [stamps.config :refer [constants]]
   [stamps.db     :refer [localStorage-key]]
   [re-frame.core :as re-frame]
   [stamps.util :as util]))

;; TODO when creating a new log, only allow either due dates or goals, not both
(defn due-date-picker
  [*date*]
  (let [_now    (js/Date.)
        today   (-> _now .toISOString (.slice 0 10))
        weekly? (r/atom false)]
    (fn [*date*]
      [:div
       [:p "Due once, on"]
       [:input#due-date
        {:type :date :min today
         :disabled @weekly?
         :on-change #(do
                       (reset! *date* (util/date-string->local-ms (-> % .-target .-value))))}]
       [:p "or"]
       [:input {:id "due-weekly" :type :checkbox :checked @weekly?
                :on-change #(let [checked (-> % .-target .-checked)]
                              (reset! weekly? checked)
                              (when-not checked
                                (set! (.-value (js/document.querySelector "#due-date")) "")
                                (.focus (js/document.getElementById "due-date"))
                                (reset! *date* nil)))}]
       [:label {:for "due-weekly" :style {:font-style :italic :font-family :sans}}
        "Due Weekly"]
       [:br]
       [:p "on"]

       (when @weekly?
         (doall
          (for [weekday-idx (range 7)]
            ^{:key weekday-idx}
            [:button
             {:on-click #(reset! *date* weekday-idx)
              :disabled (= weekday-idx @*date*)
              :style {:font-weight (if (= weekday-idx @*date*) :bold :normal)}}
             (str (nth ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"] weekday-idx) "s")])))])))

(defn goal-setter
  [*goal*]
  (let [number-of-stamps*     (r/atom 1)
        number-of-time-units* (r/atom 1)
        time-units*           (r/atom "day")
        goal-form             (fn [] (str @number-of-stamps* ":" @number-of-time-units* ":" @time-units*))
        _ (when-not @*goal* (reset! *goal* (goal-form)))] ; prevents empty goals
    (fn [*goal*]
      [:div
       {:style {}}
       [:input
        {:type :number :value @number-of-stamps* :min 1 :max 99
         :on-change #(do
                       (reset! number-of-stamps* (clamp 1 99 (int (-> % .-target .-value))))
                       (reset! *goal* (goal-form)))
         :style {:width :4ch :padding "0.15em 0.25em"}}]
       [:label " stamp" (when (> @number-of-stamps* 1)
                          [:span "s"])]
       [:p {:style {:font-style :italic :font-family :sans
                    :position :relative :left :1em}} "every"]
       [:input {:type :number :value @number-of-time-units* :min 1 :max 99
                :on-change #(do
                              (reset! number-of-time-units* (clamp 1 99 (-> % .-target .-value)))
                              (reset! *goal* (goal-form)))
                :style {:width :4ch :padding "0.15em 0.25em"}}]
       [:select
        {:on-change #(do
                       (reset! time-units* (-> % .-target .-value))
                       (reset! *goal* (goal-form)))}
        [:option {:value "day"}  (str "day"  (when (> @number-of-time-units* 1) "s"))]
        [:option {:value "week"} (str "week" (when (> @number-of-time-units* 1) "s"))]]])))

(defn log-maker
  []
  (let [new-name*    (r/atom "")
        due-on*      (r/atom nil)
        is-due-on?   (r/atom false)
        is-goal-set? (r/atom false)
        *goal*       (r/atom nil)
        reset-input! (fn []
                       (reset! new-name* "")
                       (reset! due-on* nil)
                       (reset! is-due-on? false)
                       (reset! is-goal-set? false)
                       (reset! *goal* nil))]
    (fn []
      [:div {:style {:margin "1rem 0.75rem"
                     :border "1px solid black"
                     :padding "0.5rem 1rem"
                     :display :flex
                     :flex-direction :column
                     :align-items :flex-start
                     :gap "0.5rem"
                    }}
       [:h3 "New Log"]
       [:label {:style {:font-style :italic :font-family :sans}} "Name "]
       [:input
        {:type :text
         :on-key-down (fn [e] (when (= "Enter" (.-key e))
                                (.click (js/document.getElementById "create-log-button"))))
         :value @new-name*
         :on-change #(reset! new-name* (subs (-> % .-target .-value) 0 (constants :max-log-name-length)))
         :style {:width :20ch :padding "0.15em 0.25em"}}]
       [:div
        [:input
         {:id "due-on" :type :checkbox :checked @is-due-on?
          :on-change #(let [checked (-> % .-target .-checked)]
                        (reset! is-due-on? checked)
                        (when-not checked
                          (reset! due-on* nil)))}]
        [:label {:for "due-on" :style {:font-style :italic :font-family :sans}}
         "Deadline"]
        (when @is-due-on?
          [due-date-picker due-on*])]
       [:div
        [:input
         {:id "goal" :type :checkbox :checked @is-goal-set?
          :on-change #(let [checked (-> % .-target .-checked)]
                        (reset! is-goal-set? checked)
                        (when-not checked
                          (reset! *goal* nil)))}]
        [:label {:for "goal" :style {:font-style :italic :font-family :sans}}
         "Goal"]
        (when @is-goal-set?
          [goal-setter *goal*])]
       [:button#create-log-button
        {:disabled (or
                    (empty? @new-name*)
                    false ; TODO check for date validity
                    false ; TODO check for goal validity... or is that even a thing?
                   )
         :on-click #(do
                      (rf/dispatch [::events/new-log
                                    @new-name*
                                    @due-on*
                                    @*goal*])
                      (reset-input!))}
        "Create"]]))
)

(defn- active-ledger->rf-sub
  [active-ledger]
  (case active-ledger
    :all             ::subs/log-ids-all
    :needs-attention ::subs/log-ids-needs-attention
    :done            ::subs/log-ids-done
    :overdue         ::subs/log-ids-overdue
    :archived        ::subs/log-ids-archived))

(defn- sort-param->predicate-function
  [sort-param]
  (case sort-param
    :id           :id
    :name         :name
    :stamp-count #(count (:timestamps %))
    :most-recent #(apply min (map (fn [n] (- js/Date.now n)) (:timestamps %))) ;; TODO get the minimum of now - timestamp
  ))

(defn ledger-viewer
  []
  (let [log-ids @(rf/subscribe [(active-ledger->rf-sub @(rf/subscribe [::subs/active-ledger]))])
        ;; TODO so I basically made it so that 'ALL' is the only view, and the sort does work, though to do more complex sorting
        ;; this subscription below takes a keywork and returns pairs of [log-id value]
        ;; to do more complex sorting, we'll need to be able to use a predicate function as well
        ;; that will not work with this interface
        all-logs-with-parameter @(rf/subscribe [::subs/logs-with-parameter @(rf/subscribe [::subs/sort-parameter])])
        sorted-ids (sort-by second all-logs-with-parameter)
        ordered-ids (map first (if @(rf/subscribe [::subs/reverse-sort])
                                 (reverse sorted-ids)
                                 sorted-ids))
        ]
    (when (seq ordered-ids)
      [:div {:style {:margin "1rem 0.75rem"
                     :border "1px solid black"
                     :padding "0.5rem 1rem"
                     :display :flex
                     :flex-direction :column
                     :align-items :flex-start
                     :gap "0.5rem"
                     :overflow :scroll}}
       [:h3
        "Logs"]
       (doall
        (for [log ordered-ids]
          (let [log @(rf/subscribe [::subs/log log])]
            ^{:key (:id log)}
            [:div
             [:button
              {:on-click #(re-frame/dispatch [::events/target-log (:id log)])
               :disabled (= (:id log) @(rf/subscribe [::subs/target-log]))}
              (:name log)]])))])))

(defn log-goal-display
  "Give this a log map and it will return you some hiccup
   about its goal."
  [log]
  (when (:goal log)
    (let [[num-stamps num-time-units time-unit] (str/split (:goal log) #":")
          goal-window-ms (* num-time-units (util/timespan->ms time-unit))
          now            (js/Date.now)
          goal-cutoff    (js/Date. (- now goal-window-ms))
          stamps-in-goal-window (count (filter #(>= % goal-cutoff) (:timestamps log)))

          ]
      [:div
       {:style {:padding "0.5rem 1rem"
                :border "1px solid black"}}
       [:p "Goal: Every "
        (when (> num-time-units 1)
          [:span.logGoalTimePeriodQuantity num-time-units])
        " "
        [:span.logGoalTimePeriodUnit time-unit]
        (when (> num-time-units 1) "s")]
       [:div
        [:span.logGoalStampsSoFar stamps-in-goal-window]
        [:span.logGoalDivider " / "]
        [:span.logGoalStampsRequired num-stamps]]
       (if (>= stamps-in-goal-window num-stamps)
         [:p "Goal met!"]
         [:p (- num-stamps stamps-in-goal-window) " more stamps to meet goal."])])))

(defn log-deadline-display
  "Pass this a log map and it will return you some hiccup
   about the deadline of the log, if applicable."
  [log]
  (when (:due-on log)
    (let [stamp-count (count (:timestamps log))
          last-stamped (when (pos? (count (:timestamps log)))
                         (- (js/Date.now) (first (:timestamps log))))
          due-weekly? (> 7 (:due-on log)) ; if due weekly, this value is the index of the weekday
          was-stamped-today? (and last-stamped
                                  (< last-stamped (util/ms-in-days 1)))]
      [:div
       (cond
         ; due today, incomplete
         (and (util/due-today? log) (not was-stamped-today?))
         [:span {:style {:color :red}} "Due today!!"]

         ; due today, complete
         (and (util/due-today? log) was-stamped-today?)
         [:span {:style {:color :green}} "Completed today!"]

         ; overdue ;; TODO needs better test for overdue
         ; might need to overhaul weekly logic
         ;  and how would we consider overdue for weekly logs?
         ;  this alters the completion conditional as well, potentially.
         ; (and (not due-weekly?) (not was-stamped-today?))
         ; [:span {:style {:color :red}} "Overdue!"]

         ; otherwise (not due yet)
         :else
         [:div
          (if due-weekly?
            [:div
             [:h4 "Due on " (util/idx->weekday (:due-on log)) "s"]]
            [:div
             [:h4 "Due in"]
             [:p (util/ms->timelength-string (- (:due-on log) (js/Date.now)))]])]
         )])))

(defn log-viewer
  [log-id]
  (let [log @(rf/subscribe [::subs/log log-id])]
    (if log
      (let [stamp-count (count (:timestamps log))
            last-stamped (when (pos? (count (:timestamps log)))
                           (- (js/Date.now) (first (:timestamps log))))
            was-stamped-today? (and last-stamped
                                    (< last-stamped (util/ms-in-days 1)))
           ]
        [:div#log-viewer
         {:style {:max-width :40vw
                  :margin "1rem 0.75rem"
                  :border "1px solid black"
                  :padding "0.5rem 1rem"
                  :display :flex
                  :flex-direction :column
                  :align-items :flex-start
                  :gap "0.5rem"}}
         [:h3 (:name log)]
         [:button
          {:on-click (fn [_] (let [next-label (js/prompt "New name?" (:name log))]
                               (when (and next-label
                                          (seq (.trim next-label)))
                                 (re-frame/dispatch [::events/rename-log (:id log) next-label]))))}
          "✏️  Rename"]
         (cond
           (empty? (:timestamps log))
           [:p "No stamps yet."]
           :otherwise
           [:div
            [:h4 "Last Stamped"]
            [:p (if (< (util/ms-in-seconds 30) last-stamped)
                  (util/ms->timelength-string last-stamped)
                  "A moment ago.")]])

         [log-goal-display log]
         [log-deadline-display log]

         [:button
          {:on-click #(re-frame/dispatch [::events/stamp-log log-id])}
          "STAMP"]
         [:br]
         [:p (str "(" stamp-count ") stamps")]
         [:p
          {:style {:display :flex :flex-direction :row :gap "0.25rem" :flex-wrap :wrap}}
          (doall
           (for [stamp @(rf/subscribe [::subs/log-stamps log-id])]
             ^{:key stamp}
             [:button
              {:style {:font-size :2rem}
               :on-click #(re-frame/dispatch [::events/remove-timestamp-from-log log-id stamp])}
              "⍟"]))]
         [:button
          {:on-click #(re-frame/dispatch [::events/delete-log log-id])}
          "Delete Me"]])
      ;; no log
      [:div#log-viewer
       {:style {:margin "1rem 0.75rem"
                :border "1px solid black"
                :padding "0.5rem 1rem"
                :display :flex
                :flex-direction :column
                :align-items :flex-start
                :gap "0.5rem"}}
       [:h3 "No Log Selected"]])))

(defn ledger-selector
  []
  (let []
    (fn []
      [:div
       {:style {:margin          "1rem 0.75rem"
                :display         :flex
                :flex-direction  :column
                :justify-content :space-evenly
                :align-items     :center}}
       [:span {:style {:font-size :2rem :cursor :pointer}} "👁️"]
       [:select
        {:on-change #(re-frame/dispatch [::events/set-active-ledger (-> % .-target .-value)])
         :value @(rf/subscribe [::subs/active-ledger])}
        [:option {:value :all} "All"]
        [:option {:value :needs-attention} "Needs Attention"]
        [:option {:value :archived} "Archived"]]
       [:select
        {:value @(rf/subscribe [::subs/sort-parameter])
         :on-change (fn [e] (re-frame/dispatch [::events/set-sort-parameter (-> e .-target .-value)]))}
        [:option {:value "id"} "Age"]
        [:option {:value "name"} "Name"]]
       [:label "Reverse? "
        [:input
         {:type :checkbox
          :checked @(re-frame/subscribe [::subs/reverse-sort])
          :on-change #(re-frame/dispatch [::events/toggle-reverse-sort])}]]])))

(defn main []
  (let [_ 42]
    [:div
     {:style {:margin-inline :auto
              :height :100dvh
              :background "white"
              :display :flex
              :flex-direction :column
              :justify-content :center
              :align-items  :flex-start}}
     [:div
      {:style {:display :flex :gap "1rem" :flex-wrap :wrap}}

      [:h2 {:style {:margin-left :2rem}} "Stamps"]
      [log-maker]
      [ledger-selector]
      [ledger-viewer]
      [log-viewer @(rf/subscribe [::subs/target-log])]

      [:div {:style {:display :flex :gap "1rem" :flex-wrap :wrap}}]]
     [:pre]
     [:button
      {:on-click #(when (js/confirm "Wait, really??")
                   (do
                     (js/localStorage.removeItem localStorage-key)
                     (js/window.location.reload)))}
      "Delete All Data"]]))
