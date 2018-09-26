This project holds the functional tests to be run on a Nuxeo Server distribution without the wizard, nor any Marketplace package installed.

# Running tests

To run the tests:

    mvn verify [-Denv.NUXEO_HOME=/path/to/my/nuxeo/server] [-Dmaven.failsafe.debug] [-Dit.test=test1,test2,...]

To run the suites on an already-running Nuxeo instance, use:

    mvn -o test-compile
    mvn -o org.apache.maven.plugins:maven-failsafe-plugin:integration-test -Dlog4j.configurationFile=src/main/resources/log4j2-test.xml -Dnuxeo.log.dir=target

Test results are available in target/failsafe-reports

See [tools-nuxeo-ftest documentation](https://github.com/nuxeo/tools-nuxeo-ftest).
