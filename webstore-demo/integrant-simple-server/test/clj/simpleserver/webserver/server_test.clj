(ns simpleserver.webserver.server-test
  (:require [clojure.test :refer [deftest use-fixtures is testing]]
            [clojure.tools.logging :as log]
            [clojure.data.codec.base64 :as base64]
            [integrant.core :as ig]
            [simpleserver.util.config :as ss-config]
            [simpleserver.service.service :as ss-service]
            [simpleserver.test-config :as ss-tc]
            [simpleserver.service.user.user-interface :as ss-user-i]
            [simpleserver.service.session.session-interface :as ss-session-i]
            ))

(defn init-fixture
  []
  (ss-user-i/-reset-users! (:user (ss-tc/test-service)) (ss-tc/test-env))
  (ss-session-i/-reset-sessions! (:session (ss-tc/test-service)) (ss-tc/test-env)))

(defn webserver-test-fixture
  [f]
  (log/debug "ENTER webserver-test-fixture")
  (ss-tc/test-system-fixture-runner init-fixture f)
  (log/debug "EXIT webserver-test-fixture"))

(use-fixtures :each webserver-test-fixture)

(deftest info-test
  (log/debug "ENTER info-test")
  (testing "GET: /info"
    (let [ret (ss-tc/-call-api :get "info" nil nil)]
      (is (= (ret :status) 200))
      (is (= (ret :body) {:info "index.html => Info in HTML format"})))))

(deftest signin-test
  (log/debug "ENTER signin-test")
  (testing "POST: /signin"
    ; First time should create the user since the email does not yet exist.
    (let [test-body {:first-name "mikko" :last-name "mikkonen" :password "salainen" :email "mikko.mikkonen@foo.com"}]
      (let [ret (ss-tc/-call-api :post "signin" nil test-body)]
        (is (= (ret :status) 200))
        (is (= (ret :body) {:email "mikko.mikkonen@foo.com" :ret "ok"})))
      ; Second time should fail since the email already exists.
      (let [ret (ss-tc/-call-api :post "signin" nil test-body)]
        (is (= (ret :status) 400))
        (is (= (ret :body) {:email "mikko.mikkonen@foo.com" :ret "failed" :msg "Email already exists"}))))))

(deftest login-test
  (log/debug "ENTER login-test")
  (testing "POST: /login"
    ; First with good credentials.
    (let [good-test-body {:email "kari.karttinen@foo.com" :password "Kari"}
          bad-test-body {:email "kari.karttinen@foo.com" :password "WRONG-PASSWORD"}]
      (let [ret (ss-tc/-call-api :post "login" nil good-test-body)]
        (is (= (ret :status) 200))
        (is (= (get-in ret [:body :ret]) "ok"))
        (is (= (get-in ret [:body :msg]) "Credentials ok"))
        (is (> (count (get-in ret [:body :json-web-token])) 30)))
      ; Next with bad credentials.
      (let [ret (ss-tc/-call-api :post "login" nil bad-test-body)]
        (is (= (ret :status) 400))
        (is (= (get-in ret [:body :ret]) "failed"))
        (is (= (get-in ret [:body :msg]) "Credentials are not good - either email or password is not correct"))))))


(defn -create-basic-authentication
  "A helper method to create basic authentication for ceNrtain get methods which
  require the json-web-token in header."
  [json-webtoken]
  (log/debug "ENTER -create-basic-authentication")
  (let [added-token (str json-webtoken ":NOT")
        encoded-token (apply str (map char (base64/encode (.getBytes added-token))))
        basic-str (str "Basic " encoded-token)]
    {"authorization" basic-str}))


(deftest product-groups-test
  (log/debug "ENTER product-groups-test")
  (testing "GET: /product-groups"
    (let [login-ret (ss-tc/-call-api :post "login" nil {:email "kari.karttinen@foo.com" :password "Kari"})
          _ (log/debug (str "Got login-ret: " login-ret))
          json-web-token (get-in login-ret [:body :json-web-token])
          params (-create-basic-authentication json-web-token)
          get-ret (ss-tc/-call-api :get "/product-groups" params nil)
          status (:status get-ret)
          body (:body get-ret)
          right-body {:ret "ok", :product-groups {:1 "Books", :2 "Movies"}}]
      (is (= (not (nil? json-web-token)) true))
      (is (= status 200))
      (is (= body right-body)))))

(deftest products-test
  (log/debug "ENTER products-test")
  (testing "GET: /products"
    (let [login-ret (ss-tc/-call-api :post "login" nil {:email "kari.karttinen@foo.com" :password "Kari"})
          _ (log/debug (str "Got login-ret: " login-ret))
          json-web-token (get-in login-ret [:body :json-web-token])
          params (-create-basic-authentication json-web-token)
          get-ret (ss-tc/-call-api :get "/products/1" params nil)
          status (:status get-ret)
          body (:body get-ret)
          pg-id (:pg-id body)
          ret (:ret body)
          products (:products body)]
      (is (= (not (nil? json-web-token)) true))
      (is (= status 200))
      (is (= ret "ok"))
      (is (= pg-id "1"))
      (is (= (count products) 35)))))

(deftest product-test
  (log/debug "ENTER product-test")
  (testing "GET: /product"
    (let [login-ret (ss-tc/-call-api :post "login" nil {:email "kari.karttinen@foo.com" :password "Kari"})
          _ (log/debug (str "Got login-ret: " login-ret))
          json-web-token (get-in login-ret [:body :json-web-token])
          params (-create-basic-authentication json-web-token)
          get-ret (ss-tc/-call-api :get "/product/2/49" params nil)
          status (:status get-ret)
          body (:body get-ret)
          pg-id (:pg-id body)
          p-id (:p-id body)
          ret (:ret body)
          product (:product body)
          right-product ["49" "2" "Once Upon a Time in the West" "14.4" "Leone, Sergio" "1968" "Italy-USA" "Western"]]
      (is (= (not (nil? json-web-token)) true))
      (is (= status 200))
      (is (= ret "ok"))
      (is (= pg-id "2"))
      (is (= p-id "49"))
      (is (= product right-product)))))

(comment
  (ss-tc/go)
  (ss-tc/halt)
  @ss-tc/test-system
  (user/system)

  )