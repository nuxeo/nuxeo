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

assemble-cap:
	cd nuxeo-distribution ; mvn clean install -Pnuxeo-cap,tomcat

selenium-tomcat:
	./scripts/runtests.py tomcat

selenium-cap:
	./scripts/runtests.py cap

clean:
	find . "(" -name "*~" -or -name "*.orig" -or -name "*.rej" ")" -print0 | xargs -0 rm -f
	rm -rf */target */*/target */*/*/target
	rm -rf */bin */*/bin */*/*/bin

