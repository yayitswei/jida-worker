(ns jida-worker.core
  (:use [clojure.java.shell :only [sh with-sh-dir]])
  (:require [clj-redis.client :as redis])
  (:gen-class))

(def codec-jar-path
  (or (System/getenv "CODEQ_JAR_PATH")
      "/Users/wei/projects/clojure/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar"))

(def datomic-uri
  (or (System/getenv "DATOMIC_URI")
      "datomic:free://localhost:4334/git"))

(def redis-uri
  (or (System/getenv "REDIS_URI")
      "redis://localhost"))

(def redis-conn (atom nil))
(defn db []
  (or @redis-conn
      (let [uri redis-uri]
        (reset! redis-conn (redis/init :url uri)))))

(defn success? [{exit :exit}]
  (= 0 exit))

(def working-dir "/tmp")
(defn git-clone [address]
  (success?
    (with-sh-dir
      working-dir
      (println "Cloning " address "..")
      (sh "git" "clone" address))))

(defn import-into-codeq [dirname]
  (success?
    (with-sh-dir
      (str working-dir "/" dirname)
      (println "Importing into codeq..")
      (println
        (sh "java"
            "-server" "-Xmx1g"
            "-jar" codec-jar-path
            datomic-uri))
      (println "Done."))))

(defn process-repo [address]
  (when (re-matches #"https:\/\/.*" address)
    (try
      (let [[[_ dirname]] (re-seq #".*/(.*).git" address)]
        (when (git-clone address) (import-into-codeq dirname)))
      (catch Exception e (println "ERROR: " e)))))

(defn get-tasks []
  (println "Waiting for a task..")
  (let [[_ address] (redis/blpop (db) ["tasks"] 0)]
    (process-repo address)))

(defn -main [& m]
  (println (System/getenv))
  (dorun (repeatedly get-tasks)))
