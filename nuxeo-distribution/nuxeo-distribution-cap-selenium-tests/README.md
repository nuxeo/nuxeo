# Running test suites

## Using selenium IDE on a dedicated Firefox 26

- Get a (Firefox 26)[http://ftp.mozilla.org/pub/mozilla.org/firefox/releases/26.0/linux-x86_64/en-US/]

- Create a `user-extension.xml` file in the `target` directory:


        mvn org.nuxeo.build:ant-assembly-maven-plugin:integration-test -o -Dtarget=run-selenium -Dsuites=suite1

- Shutdown firefox if another version is running


- Launch firefox 26 on a new profile, create a "selenium" profile

        /usr/local/firefox26/firefox -P

- Search and install a Firefox addon "Selenium IDE", version 2.9.0 works, restart FF

- Launch Selenium IDE from FF Tools, then from Options/Options/Selenium Core extensions
  select the `target/user-extension.xml` file.

- Close Selenium IDE, then restart, load the test suite file (tests/suite1.html) then debug


## Using maven with Firefox 3.6 (up to 26)


Tests require firefox 3.6 up to 26.

See [tools-nuxeo-ftest documentation](https://github.com/nuxeo/tools-nuxeo-ftest).

Sample usage:

    mvn verify -Pqa,[tomcat|jboss] -Dwizard.preset=nuxeo-dm -Dsuites=suite1,suite2,suite-dm
    mvn verify -Denv.NUXEO_HOME=/path/to/my/tomcat -Dsuites=suite1

To run the suites on an already-running Nuxeo instance, use:

    mvn org.nuxeo.build:ant-assembly-maven-plugin:integration-test -o -Dtarget=run-selenium -Dsuites=suite1,suite-cap

# Writing tests

See [Selenium tests documentation](http://doc.nuxeo.com/x/eQQz).
