
(ns daily-goal-app.core
  (:require [goog.dom :as dom]
            [ajax.core :as ajax]
            [rum.core :as rum]))

(js/console.log "Daily Goal App is starting...")


(def login-url "/login")

(defn get-token [] (.. (goog.dom/getElement "app") (getAttribute "xsrf")))

(defn login [un pw cb]
  (ajax/POST
   login-url
   {:handler cb
    :error-handler #(js/console.error "Error logging in:" (:status (js->clj %)) (:status-text %))
    :params {:un un
             :pw pw}
    :headers
    {"X-XSRF-Token" (get-token)}
    :format (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})}))


(defn f []
  (print "dura"))


            

