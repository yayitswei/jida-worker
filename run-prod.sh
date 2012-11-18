#!/bin/bash
REDIS_URI=redis://96.126.103.193:6379/ CODEQ_JAR_PATH=/usr/app/jikken/repos/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar DATOMIC_URI=datomic:free://96.126.103.193:4334/git java -jar jida-worker-0.1.0-SNAPSHOT-standalone.jar
