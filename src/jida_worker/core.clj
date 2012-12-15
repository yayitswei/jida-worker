(ns jida-worker.core
  (:use [clojure.java.shell :only [sh with-sh-dir]])
  (:require [taoensso.carmine :as car])
  (:gen-class))

(def codec-jar-path
  (or (System/getenv "CODEQ_JAR_PATH")
      "/Users/wei/projects/clojure/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar"))

(def datomic-uri
  (or (System/getenv "DATOMIC_URI")
      "datomic:free://localhost:4334/git"))

(def redis-uri
  (java.net.URI. (or (System/getenv "REDIS_URI")
                     "redis://96.126.103.193:6379")))
(def pool      (car/make-conn-pool))
(def conn-spec (car/make-conn-spec :host (.getHost redis-uri)
                                   :port (.getPort redis-uri)))

(defmacro wcar [& body] `(car/with-conn pool conn-spec ~@body))

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
  (let [[_ address] (wcar (car/blpop "tasks" 0))]
    (process-repo address)))

(defn -main [& m]
  (println (System/getenv))
  (dorun (repeatedly get-tasks)))
