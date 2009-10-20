#!/bin/bash

java -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=n -jar nuxeo-distribution.jar $@ 