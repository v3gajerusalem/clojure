(ns simpleserver.webserver.server
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure.data.codec.base64 :as base64]
            [ring.middleware.params :as ri-params]
            [ring.util.response :as ri-resp]
            [ring.adapter.jetty :refer [run-jetty]]
            [reitit.ring.middleware.muuntaja :as re-mu]
            [reitit.ring :as re-ring]
            [reitit.ring.coercion :as re-co]
            [reitit.coercion.spec :as re-co-spec]
            [muuntaja.core :as mu-core]
            [simpleserver.service.service :as ss-service]
            [simpleserver.service.domain.domain-interface :as ss-domain-i]
            [simpleserver.service.user.user-interface :as ss-user-i]
            [simpleserver.service.session.session-interface :as ss-session-i]))

;; Use curl and simple server log to see how token is parsed.
;; Or use this trick: You got a JSON web token from -login. Supply JSON web token to:
;; (simpleserver.webserver.server/-create-testing-basic-authentication-from-json-webtoken "<token" )
;; I.e. (simpleserver.webserver.server/-valid-token? (simpleserver.webserver.server_test/-create-testing-basic-authentication-from-json-webtoken "<token>"))


(defn -valid-token?
  "Parses the token from the http authorization header and asks session ns to validate the token."
  [env req]
  (log/debug "ENTER -valid-token?")
  (let [basic (get-in req [:headers "authorization"])
        ;_ (log/debug (str "basic: " basic))
        basic-str (and basic
                       (last (re-find #"^Basic (.*)$" basic)))
        raw-token (and basic-str
                       (apply str (map char (base64/decode (.getBytes basic-str)))))
        ;_ (log/debug (str "raw-token: " raw-token))
        ; Finally strip the password part if testing with curl
        token (and raw-token
                   (string/replace raw-token #":NOT" ""))]
    ;; Session namespace does the actual validation logic.
    ;; Note: clj-kondo complains if else branch is missing.
    (if token
      (ss-session-i/validate-token (ss-service/get-service env :session) env token)
      nil)))

;; As in headers check with curl that the http status is properly set.
(defn -set-http-status
  "Sets the http status either to 200 (ret=ok) or 400 (otherwise)."
  [ring-response ret]
  (if (= ret :ok)
    ring-response
    (ri-resp/status ring-response 400)))


(defn -info
  "Gets the info."
  [_]
  (log/debug "ENTER -info")
  {:status 200 :body {:info "index.html => Info in HTML format"}})

(defn -validate-parameters
  "Extremely simple validator - just checks that all fields must have some value.
  `field-values` - a list of fields to validate."
  [field-values]
  (every? #(seq %) field-values))

(defn -signin
  "Provides API for sign-in page."
  [env first-name last-name password email]
  (log/debug "ENTER -signin")
  (let [validation-passed (-validate-parameters [email first-name last-name password])
        response-value (if validation-passed
                         (ss-user-i/add-new-user (ss-service/get-service env :user) env email first-name last-name password)
                         {:ret :failed, :msg "Validation failed - some fields were empty"})]
    (-set-http-status (ri-resp/response response-value) (:ret response-value))))

;        (comment        credentials-ok (if (validation-passed  1)))

(defn -login
  "Provides API for login page."
  [env email password]
  (log/debug "ENTER -login")
  (let [validation-passed (-validate-parameters [email password])
        credentials-ok (if validation-passed
                         (ss-user-i/credentials-ok? (ss-service/get-service env :user) env email password)
                         nil)
        json-web-token (if credentials-ok
                         (ss-session-i/create-json-web-token (ss-service/get-service env :session) env email)
                         nil)
        response-value (if (not validation-passed)
                         {:ret :failed, :msg "Validation failed - some fields were empty"}
                         (if (not credentials-ok)
                           {:ret :failed, :msg "Credentials are not good - either email or password is not correct"}
                           (if (not json-web-token)
                             {:ret :failed, :msg "Internal error when creating the json web token"}
                             {:ret :ok, :msg "Credentials ok" :json-web-token json-web-token})))]
    (-set-http-status (ri-resp/response response-value) (:ret response-value))))

(defn -product-groups
  "Gets product groups."
  [env req]
  (log/debug "ENTER -product-groups")
  (let [token-ok (-valid-token? env req)
        response-value (if token-ok
                         {:ret :ok, :product-groups (ss-domain-i/get-product-groups (ss-service/get-service env :domain) env)}
                         {:ret :failed, :msg "Given token is not valid"})]
    (-set-http-status (ri-resp/response response-value) (:ret response-value))))

(defn -products
  "Gets products."
  [env req]
  (log/debug "ENTER -products")
  (let [pg-id (get-in req [:path-params :pg-id])
        token-ok (-valid-token? env req)
        response-value (if token-ok
                         {:ret :ok, :pg-id pg-id :products (ss-domain-i/get-products (ss-service/get-service env :domain) env pg-id)}
                         {:ret :failed, :msg "Given token is not valid"})]
    (-set-http-status (ri-resp/response response-value) (:ret response-value))))

(defn -product
  "Gets product."
  [env req]
  (log/debug "ENTER -product")
  (let [pg-id (get-in req [:path-params :pg-id])
        p-id (get-in req [:path-params :p-id])
        token-ok (-valid-token? env req)
        response-value (if token-ok
                         {:ret     :ok,
                          :pg-id   pg-id
                          :p-id    p-id
                          :product (ss-domain-i/get-product (ss-service/get-service env :domain) env pg-id p-id)}
                         {:ret :failed, :msg "Given token is not valid"})]
    (-set-http-status (ri-resp/response response-value) (:ret response-value))))

(defn routes
  "Routes."
  [env]
  [
   ["/info" {:get {:handler (fn [{}] (-info env))
                   :responses {200 {:description ""}}}}]
   ["/print-req-get/:jee" {:get (fn [req] (prn (str "req: ") req))}] ; An example how to print the ring request
   ["/print-req-post" {:post (fn [req] (prn (str "req: ") req))}] ; An example how to print the ring request
   ["/signin" {:post (fn [{{:keys [first-name last-name password email]} :body-params}] (-signin env first-name last-name password email))}]
   ["/login" {:post (fn [{{:keys [email password]} :body-params}] (-login env email password))}]
   ["/product-groups" {:get {:handler (fn [req] (-product-groups env req))}}]
   ["/products/:pg-id" {:get {:handler (fn [req] (-products env req))}}]
   ["/product/:pg-id/:p-id" {:get {:handler (fn [req] (-product env req))}}]
   ])

;; NOTE: If you want to check what middleware does you can uncomment rows 67-69 in:
;; https://github.com/metosin/reitit/blob/master/examples/ring-swagger/src/example/server.clj#L67-L69

(defn handler
  "Handler."
  [{:keys [routes]}]
  (re-ring/ring-handler
    (re-ring/router routes
                    {:data {:muuntaja   mu-core/instance
                            :coercion   re-co-spec/coercion
                            :middleware [ri-params/wrap-params
                                         re-mu/format-middleware
                                         re-co/coerce-exceptions-middleware
                                         re-co/coerce-request-middleware
                                         re-co/coerce-response-middleware]}})
    (re-ring/routes
      (re-ring/create-resource-handler {:path "/"})
      (re-ring/create-default-handler))))


(comment
  (user/system)
  (handler {:routes (routes (user/env))})

  )