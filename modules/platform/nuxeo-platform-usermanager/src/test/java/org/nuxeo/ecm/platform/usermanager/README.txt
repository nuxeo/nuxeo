1. In order to run the remote tests one should change the paths found in
testrun.properties to match his own environment.

2. You also need to have NXCoreAPI module deployed ( and all the required modules )
and a JBoss instance running to run the remote tests.

3. The following jars are needed in the junit test classpath:

- jboss-aop-jdk50-client.jar
- jboss-aspect-jdk50-client.jar
- concurrent.jar
