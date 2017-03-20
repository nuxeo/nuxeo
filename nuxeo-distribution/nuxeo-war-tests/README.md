This project holds the functional tests to be run on a
Tomcat with Nuxeo deployed as a WAR inside

See: (doc.nuxeo.com/nxdoc/deploying-as-a-standard-static-war/)[https://doc.nuxeo.com/nxdoc/deploying-as-a-standard-static-war/]

To run the integration tests:

    mvn verify [-Dwebdriver.firefox.bin="/path/to/firefox"]

Test results are available in target/failsafe-reports
