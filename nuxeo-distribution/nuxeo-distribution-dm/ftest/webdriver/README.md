This project holds the WebDriver functional tests to be run on a
Nuxeo distribution without the wizard and with the DM module installed.

# Running tests

Tests require firefox 5 or higher.

To run the tests:

    mvn verify [-Denv.NUXEO_HOME=/path/to/my/nuxeo/server] [-Dmaven.failsafe.debug] [-Dit.test=test1,test2,...]

Test results are available in target/failsafe-reports

See [tools-nuxeo-ftest documentation](https://github.com/nuxeo/tools-nuxeo-ftest).

# Writing tests

See [WebDriver tests documentation](http://doc.nuxeo.com/x/5YeN).
