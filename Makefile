clean:
	mvn clean
	mvn clean -Pjetty
	mvn clean -Pgf3
	mvn clean -Ptomcat
	rm -rf test

jetty:
	mvn install -Pjetty

tomcat:
	mvn package -Ptomcat

gf3:
	mvn install -Pgf3
