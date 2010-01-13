VERSION:=1.6.1
POM_VERSION:=$(VERSION)-SNAPSHOT
TODAY:=$(shell date +"%Y%m%d")

#RELEASE:=$(VERSION)-$(TODAY)
#RELEASE_DIR:=cmissh-$(VERSION)-$(TODAY)
RELEASE:=$(TODAY)
RELEASE_DIR:=cmissh-$(TODAY)

.PHONY: all clean build release

all:	build test

build:
	mvn package

test:
	./run.sh -t -b testscript

clean:
	mvn clean
	find . -name "*~" | xargs rm -f
	rm -rf release

release: clean build
	mkdir -p target/$(RELEASE_DIR)
	cp scripts/* target/$(RELEASE_DIR)
	cp README.txt target/$(RELEASE_DIR)
	cp target/nuxeo-chemistry-shell-$(POM_VERSION).jar \
		target/$(RELEASE_DIR)/nuxeo-chemistry-shell.jar
	cd target ; zip -r $(RELEASE_DIR).zip $(RELEASE_DIR)

