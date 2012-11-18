#!/bin/bash
REDISTOGO_URL=redis://redistogo:dfcab96405c0df37b9e0088d69995010@gar.redistogo.com:9184/ CODEQ_JAR_PATH=/usr/app/jikken/repos/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar DATOMIC_URI=datomic:free://96.126.103.193:4334/git java -jar jida-worker-0.1.0-SNAPSHOT-standalone.jar
