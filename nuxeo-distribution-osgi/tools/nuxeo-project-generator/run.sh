#!/bin/sh

java -cp target/nuxeo-project-gen-1.0.jar org.nuxeo.build.osgi.gen.ProjectGenerator "-clean" "../../../../" "../../projects/pom.xml" "../../projects"