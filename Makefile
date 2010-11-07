all: build assemble-tomcat selenium-tomcat

build-with-tests:
	mvn install
	cd addons ; mvn install

build:
	mvn install -Dmaven.test.skip=true
	cd addons ; mvn install -Dmaven.test.skip=true

assemble-tomcat:
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-dm,tomcat

assemble-jboss:
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-dm,jboss

selenium-tomcat:
	./runtests.sh

clean:
	find . "(" -name "*~" -or -name "*.orig" -or -name "*.rej" ")" -print0 | xargs -0 rm -f
	rm -rf */target */*/target */*/*/target
	rm -rf */bin */*/bin */*/*/bin

