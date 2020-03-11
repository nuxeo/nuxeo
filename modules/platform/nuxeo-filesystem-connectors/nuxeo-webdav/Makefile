	APPS=$(HOME)/apps
	JBOSS=$(APPS)/nuxeo-dm-jboss-dav
	TOMCAT=$(APPS)/nuxeo-dm-tomcat-dav
	JETTY=$(APPS)/nuxeo-dm-jetty

	EAR=$(JBOSS)/server/default/deploy/nuxeo.ear

	VERSION=0.0.1-SNAPSHOT
	JAR=nuxeo-webdav-$(VERSION).jar

.PHONY: all check deploy clean start build deploy test

all: build

clean:
	rm -rf .clover bin target data data-bt *.log projectFilesBackup zip
	find . -name "*~" | xargs rm -f
	#mvn $(MVN_OPT) clean

superclean: clean
	mvn dependency:purge-local-repository

deploy-jboss: build
	mkdir -p $(EAR)/plugins
	cp target/$(JAR) $(EAR)/plugins/
	cp target/classes/lib/*.jar $(EAR)/lib/

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

zip: build
	mkdir -p zip/nuxeo-webdav/addons
	mkdir -p zip/nuxeo-webdav/lib
	cp README.txt zip/nuxeo-webdav
	cp target/nuxeo-webdav-0.0.1-SNAPSHOT.jar zip/nuxeo-webdav/addons
	cp target/classes/lib/asm-3.1.jar zip/nuxeo-webdav/lib
	cp target/classes/lib/webdav-jaxrs-1.1.jar zip/nuxeo-webdav/lib
	cd zip ; zip -r nuxeo-webdav.zip nuxeo-webdav
	cp zip/nuxeo-webdav.zip $(HOME)/Dropbox/Public/
