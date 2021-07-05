#!/bin/bash
./updatecheck.sh || exit 1
echo Starting minecraft...

source start.config
java -Dcyan.backupload=backup $JVM -jar server.jar
if [ "$?" != "0" ] && [ "$?" != "130" ]; then ./start.sh; fi
