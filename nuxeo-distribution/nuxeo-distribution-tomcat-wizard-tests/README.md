This project holds the functional tests to be run on a
Nuxeo Tomcat distribution with the wizard not done.

Tests require firefox 5 or higher.

To run the integration tests:

    mvn verify [-Denv.NUXEO_HOME=/path/to/my/nuxeo/server] [-Dmaven.failsafe.debug] [-Dit.test=test1,test2,...]

Test results are available in target/failsafe-reports
