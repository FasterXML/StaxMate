#!/bin/sh

java \
 -Xmx64M -XX:CompileThreshold=1000 -server \
 -Djetty.home=./jetty \
 -Djetty.port=7272 -jar jetty/start.jar \
jetty/jetty.xml 
