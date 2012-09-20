#!/bin/sh

java \
 -Xmx64M -XX:CompileThreshold=1000 -server \
-jar build/uuid-client.jar $* http://localhost:7272/uuid-server/generate-uuid
