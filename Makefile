all: build-tomcat test-selenium

test-unit:
	mvn test

build-all:
	mvn install -Dmaven.test.skip=true
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-dm,jboss
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-dm,tomcat

build-tomcat:
	mvn install -Dmaven.test.skip=true
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-dm,tomcat

build-jboss:
	mvn install -Dmaven.test.skip=true
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-dm,jboss

test-selenium:
	./runtests.sh

clean:
	find . "(" -name "*~" -or -name "*.orig" -or -name "*.rej" ")" -print0 | xargs -0 rm -f
	rm -rf */target */*/target */*/*/target
	rm -rf */bin */*/bin */*/*/bin

