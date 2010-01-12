.PHONY: all clean build

all:	build test

build:
	mvn package

test:
	./run.sh -t -b testscript

clean:
	mvn clean
	rm *~

