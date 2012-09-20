#!/bin/sh

java -Xmx32M -server\
 -cp lib/stax-api-1.0.1.jar:\
:lib/wstx/woodstox-core-asl-4.0.1.jar:\
:lib/stax2-api-3.0.1.jar:\
:target/classes \
$*
