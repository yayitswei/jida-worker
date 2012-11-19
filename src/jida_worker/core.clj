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
  "Returns the existing redis connection or establishes a new one"
  (or @redis-conn
      (reset! redis-conn (redis/init :url redis-uri))))

(defn git-clone [repo-address destination]
  "Clones a given REPO-ADDRESS to DESTINATION via cd'ing into the directory and shelling out to git. Returns the absolute directory of the clone git repositor."
  (log "Git cloning into " destination)
  (let [[[_ dirname]] (re-seq #".*/(.*).git" repo-address)]
    (sh "git" "clone" repo-address)
    (str (clojure.string/trim (:out (sh "pwd"))) "/" dirname)))

(defn codeq-import! []
  "Runs the codeq-importer and analyzer in one go. You must have previously cd'ed into the git repo's directory."
  (codeq/main datomic-uri))

(defn process-repo [repo-address]
  "Given a repo address, import and analyze it via codeq, and store the result in a datomic database. Can be run multiple time idempotently."
  (with-sh-dir (fs/temp-dir "jida-worker-")
    (sh "cd" (git-clone repo-address "."))
    (codeq-import!)))

(defn get-tasks []
  "Retrieves a task from the queue via a BLOCKING call"
  (log "Waiting for a task..")
  (let [[_ address] (redis/blpop (db) ["tasks"] 0)]
    (log "Processing repo at " address)
    (let [result (process-repo address)]
      (log "Finished processing repo with result: " result))))

(defn -main [& m]
  (log (System/getenv))
  (dorun (repeatedly get-tasks)))
