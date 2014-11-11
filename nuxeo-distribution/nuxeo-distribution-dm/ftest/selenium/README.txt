=================
Selenium Nuxeo EP
=================

Running test suites
===================

Tests require firefox 3.6 or higher.

See documentation at https://github.com/nuxeo/tools-nuxeo-ftest

Sample usage:

    mvn verify -Pqa,[tomcat|jboss] -Dwizard.preset=nuxeo-dm -Dsuites=suite1,suite2,suite-dm
    mvn verify -Denv.NUXEO_HOME=/path/to/my/tomcat -Dsuites=suite1

Writing tests
=============

See documentation at http://doc.nuxeo.com/x/eQQz
