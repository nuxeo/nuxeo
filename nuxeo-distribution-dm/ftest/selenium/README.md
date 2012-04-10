# Running test suites

Tests require firefox 3.6 or higher.

See [tools-nuxeo-ftest documentation](https://github.com/nuxeo/tools-nuxeo-ftest).

Sample usage:

    mvn verify -Pqa,[tomcat|jboss] -Dwizard.preset=nuxeo-dm -Dsuites=suite1,suite2,suite-dm
    mvn verify -Denv.NUXEO_HOME=/path/to/my/tomcat -Dsuites=suite1

To run the suites on an already-running Nuxeo instance, use:

    mvn org.nuxeo.build:nuxeo-distribution-tools:integration-test -o -Dtarget=run-selenium -Dsuites=suite1,suite-cap

# Writing tests

See [Selenium tests documentation](http://doc.nuxeo.com/x/eQQz).
