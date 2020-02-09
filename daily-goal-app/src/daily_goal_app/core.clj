(ns daily-goal-app.core
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [ring.middleware.session.cookie :as cookie]
            [clojure.data.json :as json])
  (:gen-class))

(def index-page "<html><head><title>Daily App</title></head><body>
                 <div id=\"app\" xsrf=\"{xsrf}\">
                 <h1>Daily App</h1></div>
                 <script src=\"/cljs-out/app.js\" type=\"text/javascript\"></script>
                 </body></html>")

(defn index-get [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (clojure.string/replace index-page #"\{xsrf\}"
                                 (::csrf/anti-forgery-token request))})
             
(defn login-post [request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :session {:logged-in true}})
   
;; Routes

(def routes
  (route/expand-routes
   #{["/" :get index-get :route-name :index-get]
     ["/login" :post login-post :route-name :login-post]}))

(def service-map
  {::http/routes routes
   ::http/type   :jetty
   ::http/port   8890
   ::http/secure-headers {:content-security-policy-settings
                          {:default-src
                           "* 'unsafe-inline' 'unsafe-eval'"
                           :script-src
                           "* 'unsafe-inline' 'unsafe-eval'"}}
   ::http/enable-csrf {}
   ::http/resource-path "/public"
   ::http/file-path "target/public"
   ::http/enable-session {:store (cookie/cookie-store {:key "a 16-byte secret"})
                          :cookie-attrs {:max-age (* 60 60 24)}}
   ::http/host   "0.0.0.0"})

(defn service
  [service-map]
  (-> service-map
      (http/default-interceptors)
      (update ::http/interceptors conj (middlewares/file-info))))

(defn start []
  (http/start (http/create-server (service service-map))))

(defonce server (atom nil))

(defn start-dev []
  (reset! server
          (http/start (http/create-server
                       (assoc (service service-map)
                              ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(defn -main [& args]
  (start-dev))

