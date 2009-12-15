	APPS=/Users/fermigier/apps
	EAR=$(APPS)/nuxeo-distribution-jboss-5.3.0/server/default/deploy/nuxeo.ear
	TOMCAT=$(APPS)/nuxeo-distribution-tomcat
	JETTY=$(APPS)/nuxeo-distribution-jetty-5.3.0

.PHONY: all check deploy clean start build deploy test

all: build

clean:
	rm -rf .clover bin target data data-bt *.log projectFilesBackup
	find . -name "*~" | xargs rm -f
	#mvn $(MVN_OPT) clean

superclean: clean
	mvn dependency:purge-local-repository

deploy-jboss: build
	cp target/nuxeo-rest-0.0.1-SNAPSHOT.jar $(EAR)/plugins
	cp target/classes/lib/*.jar $(EAR)/lib

deploy-tomcat: build
	cp target/nuxeo-rest-0.0.1-SNAPSHOT.jar $(TOMCAT)/nxserver/bundles
	cp target/classes/lib/*.jar $(TOMCAT)/nxserver/lib

deploy-jetty: build
	cp target/nuxeo-rest-0.0.1-SNAPSHOT.jar $(JETTY)/bundles
	cp target/classes/lib/*.jar $(JETTY)/lib

build:
	mvn $(MVN_OPT) clean install -Dmaven.test.skip=true

run:
	mvn clean test-compile ; ./run.sh
