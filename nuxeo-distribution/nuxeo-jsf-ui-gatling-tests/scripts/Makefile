.PHONY: clean import import-docs import-folders setup inject


all: inject setup import-folders import-docs

import-docs:
	$(info ### Creating documents...)
	mvn -f ../pom.xml -nsu gatling:execute -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim20CreateDocuments

import-folders:
	$(info ### Creating folder layout...)
	mvn -f ../pom.xml -nsu gatling:execute -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim10CreateFolders

setup:
	$(info ### Setup Nuxeo users...)
	mvn -f ../pom.xml -nsu gatling:execute -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim00Setup

inject:
	$(info ### Injecting all data into Redis...)
	python ./inject-arbre.py | redis-cli -n 7 --pipe

clean:
	find . "(" -name "*~" -or  -name ".#*" -or  -name "#*#" -or -name "*.pyc" ")" -print0 | xargs -0 rm -f
