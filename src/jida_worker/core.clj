(ns jida-worker.core
  (:use [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clj-redis.client :as redis]
            [fs.core :as fs]
            [datomic.codeq.core :as codeq])
  (:gen-class))

(defn log [& messages]
  (apply println messages))

(def redis-uri
  (or (System/getenv "REDIS_URI")
      (System/getenv "REDISTOGO_URL")
      "redis://localhost:6379/"))

(def codeq-jar-path
  (or (System/getenv "CODEQ_JAR_PATH")
      "/Users/wei/projects/clojure/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar"))

(def datomic-uri
  (or (System/getenv "DATOMIC_URI")
      "datomic:free://localhost:4334/git"))

(def redis-conn (atom nil))

(defn db []
  (or @redis-conn
      (reset! redis-conn (redis/init :url redis-uri))))

(defn git-clone [repo-address destination]
  (log "Git cloning into " destination)
  (let [[[_ dirname]] (re-seq #".*/(.*).git" repo-address)]
    (sh "git" "clone" repo-address)
    (str (clojure.string/trim (:out (sh "pwd"))) "/" dirname)))

(defn codeq-import [source-path codeq-jar-path datomic-uri]
  (log "Running Codeq on " (clojure.string/trim source-path) "...")
  (sh "cd" (clojure.string/trim source-path))
  (let [cmd ["java"
           "-server" "-Xmx1g"
           "-jar" codeq-jar-path
           datomic-uri]]
    (log (apply sh cmd))))

(defn process-repo [repo-address]
  (with-sh-dir (fs/temp-dir "jida-worker-")
    (let [cloned-dir (git-clone repo-address ".")
          cd (sh "cd" cloned-dir)
          conn '(codeq/ensure-db datomic-uri)
          [repo-uri repo-name] '(codeq/get-repo-uri)]
      (codeq/main datomic-uri))))

(defn get-tasks []
  (log "Waiting for a task..")
  (let [[_ address] (redis/blpop (db) ["tasks"] 0)]
    (log "Processing repo at " address)
    (let [result (process-repo address)]
      (log "Finished processing repo with result: " result))))

(defn -main [& m]
  (log (System/getenv))
  (dorun (repeatedly get-tasks)))
