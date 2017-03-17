This project holds the functional tests to be run on a
Tomcat with Nuxeo deployed as a WAR inside

To run the integration tests:

    mvn verify [-Dwebdriver.firefox.bin="/path/to/firefox"] [-Denv.NUXEO_HOME=/path/to/my/nuxeo/server] [-Dmaven.failsafe.debug] [-Dit.test=test1,test2,...]

Test results are available in target/failsafe-reports
