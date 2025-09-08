(ns stamps.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [stamps.events :as events]
   [stamps.views :as views]
   [stamps.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
 ; prep for install!
  (.addEventListener js/window "beforeinstallprompt"
                     (fn [e] (.preventDefault e)
                             (re-frame/dispatch [::events/set-install-event e])))
  (when (and (not config/debug?)
             (.-serviceWorker js/navigator)) ; register service worker
    (.register (.-serviceWorker js/navigator) "/stamps/gh-pages-sw.js"))
  (mount-root))
