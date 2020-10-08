(ns simplefrontend.http
  (:require [re-frame.core :as re-frame]
            [ajax.core :as ajax]))

;; See: https://github.com/day8/re-frame-http-fx
(defn post [db uri data on-success on-failure]
  {:http-xhrio {:method :post
                :uri uri
                :params data
                :format (ajax/json-request-format)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [on-success]
                :on-failure [on-failure]}
   :db db})
