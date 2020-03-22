(ns daily-goal-app.data
  (:require [clojure.java [io :as io]]
            [cognitect.aws.client.api :as aws]
;;            [clojure.core.cache.wrapped :as c]
            [taoensso.nippy :as nippy])
  (:gen-class))


(defrecord Record
    [email pwdhash routines])

(defrecord Routine
    [title minutes])

(def r1 (Record. "tylerlrhodes@gmail.com" "123"
                 [(Routine. "eat" 15) (Routine. "sleep"(* 6 60))]))

(defn stream->bytes [stream]
  (with-open [xout (java.io.ByteArrayOutputStream.)]
    (io/copy stream xout)
    (.toByteArray xout)))

(defn get-bytes [x]
  (nippy/freeze x))

(defn get-object [bytes]
  (nippy/thaw bytes))


(def s3 (aws/client {:api :s3}))

(def bucket-name
  (or (System/getenv "daily_goal_app_s3_bucket")
      "daily-app-local-bucket-store"))

(defn store-record [rec]
  (aws/invoke s3 {:op :PutObject
                  :request
                  {:Bucket bucket-name
                   :Key (:email rec)
                   :body (get-bytes rec)}}))

(defn record-exists-for? [email pwd]
  (if (:Error
       (aws/invoke s3
                   {:op :GetObject
                    :request
                    {:Bucket bucket-name
                     :Key email}}))
    false
    true))


