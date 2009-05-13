	SERVER=nxserver
	#SERVER=/opt/jboss/server/default

	LOGIN=Administrator
	PASSWORD=Administrator
	ROOT_URL=http://localhost:8080/blogs

.PHONY: check deploy clean start jar deploy-jar test classpath

ALL: deploy

jar:
	mvn clean install

deploy: deploy-jar deploy-lib

deploy-jar: jar
	cp target/nuxeo-webdav-*SNAPSHOT.jar $(SERVER)/bundles

deploy-lib: jar
	cp target/*.dir/[a-m]* $(SERVER)/lib
	cp target/*.dir/[o-z]* $(SERVER)/lib

clean:
	find . -name "*~" | xargs rm -f
	find . -name "*orig" | xargs rm -f
	mvn clean

#
# Start / stop / run
#

check: deploy
	cd $(SERVER) ; sh nxserverctl.sh start

start:
	chmod 755 $(SERVER)/*.sh
	rm -f $(SERVER)/log/*
	cd $(SERVER) ; sh nxserverctl.sh start

stop:
	cd $(SERVER) ; sh nxserverctl.sh stop ; echo "(ignore)"

restart:
	cd $(SERVER) ; sh nxserverctl.sh stop
	cd $(SERVER) ; sh nxserverctl.sh start
	sleep 1

run:
	cd $(SERVER) ; sh nxserver.sh -dev

