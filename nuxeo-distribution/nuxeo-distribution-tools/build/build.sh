#!/bin/bash

java -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n -jar ../target/nuxeo-distribution.jar $@ 