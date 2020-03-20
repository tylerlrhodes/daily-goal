
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
    :response-format (ajax/json-response-format {:keywords? false})}))


(defn f []
  (print "dura"))

(rum/defcs login-component <
  (rum/local "" ::username)
  (rum/local "" ::password)
  (rum/local "" ::status-msg)
  [state]
  (let [username (::username state)
        password (::password state)
        status-msg (::status-msg state)
        handle-login
        (fn [resp]
          (js/console.log resp))]
    [:div
     [:h1 "Login"]
     [:hr]
     [:form
      [:label "Username:"]
      [:input
       {:type "text"
        :on-change (fn [evt]
                     (reset! username (.. evt -target -value)))}]
      [:br]
      [:label "Password:"]
      [:input
       {:type "text"
        :on-change (fn [evt]
                     (reset! password (.. evt -target -value)))}]]
      [:button
       {:class "label"
        :on-click #(login @username @password handle-login)}
       "Login"]
      [:br]
      [:span "Status: " @status-msg]]))

(rum/mount (login-component nil) (dom/getElement "app"))
            

