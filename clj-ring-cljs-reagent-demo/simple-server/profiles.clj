;; **********************************************
;; Environment related profiles (choose only one)

{

 :dev
 {
  ;:plugins [[test2junit "1.3.3"]]
  :dependencies [[javax.servlet/servlet-api "2.5"]
                 [ring/ring-mock "0.3.0"]]}


 ;; For running in single-node version (internal database simulation).
 :single-node
 {
  :env {
        :ss-env "single-node"
        :my-env "dev"
        }
  }


 ;; For local dynamodb development database.
 :local-dynamodb
 {
  :env {
        :ss-env     "local-dynamodb"
        :my-env     "dev"
        ; NOTE: Endpoint format was tricky to realize.
        :endpoint   "http://dynamodb.eu-west-1.localhost:8000"
        ; Local DynamoDB: dummy but must be the same length as real keys.
        :access-key "XXXXXXXXXXXXXX___NOT"
        :secret-key "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX___NOT"
        }
  }


 ;; For real AWS DynamoDB - dev env.
 :aws-dynamodb-dev
 {
  :env {
        :ss-env      "aws-dynamodb"
        :my-env      "dev"
        :aws-profile "tmv-test"
        }
  }


;; For real AWS DynamoDB - prod env.
 :aws-dynamodb-prod
 {
  :env {
        :ss-env      "aws-dynamodb"
        :my-env      "prod"
        :aws-profile "tmv-prod"
        }
  }


;; For Azurite, i.e. for Azure Table Storage local testing.
;; NOTE: Stopped working with Azurite since it may not be reliable. See README.md
; :local-table
; {
;  :env {
;        :ss-env     "local-table"
;        :my-env     "dev"
;        ; NOTE: Endpoint for Azurite is hard coded.
;        :endpoint   "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1;"
;        }
;  }


;; For real Azure Table Storage Service - dev env.
 :azure-table-storage-dev
 {
  :env {
        :ss-env     "azure-table-storage"
        :my-env     "dev"
        ;; Read AZURE_STORAGE_ACCOUNT and AZURE_STORAGE_KEY
        ;; dynamically from environmental variables.
        :endpoint   ""
        }
  }


 ;; For real Azure Table Storage Service - prod env.
 :azure-table-storage-prod
 {
  :env {
        :ss-env     "azure-table-storage"
        :my-env     "prod"
        ;; Read AZURE_CONNECTION_STRING
        ;; Old: AZURE_STORAGE_ACCOUNT and AZURE_STORAGE_KEY
        ;; dynamically from environmental variables.
        :endpoint   ""
        }
  }



 ;; **********************************************
 ;; Logging profiles, choose only one.

 :log-dev
 {
  :resource-paths
  ["logconfig/dev"]}


 :log-prod
 {
  :resource-paths
  ["logconfig/prod"]}}




;; **********************************************
;; For repl development (always choose in IDEA Run Configuration).
;; Add this to your ~/.lein/profiles.clj:
;; :dependencies [[org.clojure/tools.namespace "0.2.11"]]
;; Then you are able to use:
;; (require '[clojure.tools.namespace.repl :refer [refresh]]) (refresh)
;; See: https://github.com/clojure/tools.namespace
;;

