	APPS=$(HOME)/apps
	EAR=$(APPS)/nuxeo-dm-jboss/server/default/deploy/nuxeo.ear
	TOMCAT=$(APPS)/nuxeo-dm-tomcat
	JETTY=$(APPS)/nuxeo-dm-jetty

	VERSION=0.0.1-SNAPSHOT
	JAR=nuxeo-webdav-$(VERSION).jar

.PHONY: all check deploy clean start build deploy test

all: build

clean:
	rm -rf .clover bin target data data-bt *.log projectFilesBackup
	find . -name "*~" | xargs rm -f
	#mvn $(MVN_OPT) clean

superclean: clean
	mvn dependency:purge-local-repository

deploy-jboss: build
	cp target/$(JAR) $(EAR)/plugins
	cp target/classes/lib/*.jar $(EAR)/lib

deploy-tomcat: build
	cp target/$(JAR) $(TOMCAT)/nxserver/bundles
	cp target/classes/lib/*.jar $(TOMCAT)/nxserver/lib

deploy-jetty: build
	cp target/$(JAR) $(JETTY)/bundles
	cp target/classes/lib/*.jar $(JETTY)/lib

build:
	mvn $(MVN_OPT) clean install -Dmaven.test.skip=true

run:
	mvn clean test-compile ; ./run.sh
