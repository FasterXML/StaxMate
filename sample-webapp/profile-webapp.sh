#!/bin/sh

# Let's do very basic profiling using JVM's in-built capabilities

java -server \
-Xmx64m -XX:CompileThreshold=200 -Xrunhprof:cpu=samples,depth=8 \
-Djetty.home=./jetty \
 -Djetty.port=7272 -jar jetty/start.jar \
jetty/jetty.xml 
