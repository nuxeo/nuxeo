# This project holds the functional tests to be run on a
# Nuxeo tomcat distribution with the wizard done and the DM module installed.

To run the integration tests:
  mvn verify [-Denv.NUXEO_HOME=/path/to/my/nuxeo/server] [-Dmaven.failsafe.debug] [-Dit.test=test1,test2,...]

Test results are available in target/failsafe-reports
