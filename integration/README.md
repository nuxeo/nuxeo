# VCS Tests

Database setup for VCS tests.

The setup is consistent with [tools-nuxeo-ftest](https://github.com/nuxeo/tools-nuxeo-ftest) (`org.nuxeo:nuxeo-ftest`, `nuxeo-ftest.xml`) and uses the same environment variables and Maven profiles.

## Introduction

### Principle

Automatically setup environment variables for testing purpose and persist those variables for using them as well in unit, functional and integration tests.

### Details and keypoints

Here is an overview of the execution timeline with some keypoints.


During the Maven **`initialize`** phase, if executed from root `org.nuxeo:nuxeo-ecm`, the testing parameters are evaluated according to the conventions:

- with the **`customdb`** Maven profile activated
    - [ant-assembly-maven-plugin](https://github.com/nuxeo/ant-assembly-maven-plugin) executes **`setup-vcs`** instructions from `integration/vcstests.xml`
        - **`vcs-tests-setup`**  
        Generates the effective database parameters and stores them in `${user.home}/nuxeo-test-vcs.properties` for later use: typically the functional tests driven by [org.nuxeo:nuxeo-ftest](https://github.com/nuxeo/tools-nuxeo-ftest)
        - **`prepare-db`**  
          Downloads the driver, checks the connection, drops and creates the testing database
    - [properties-maven-plugin](https://www.mojohaus.org/properties-maven-plugin/) executes **`read-project-properties`** instructions.  
          Reads the parameters from `${user.home}/nuxeo-test-vcs.properties` and shares them with Java as System properties.

During the Maven **`test`** phase, regardless where it is executed from:

- [maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) reads the Java System properties
- [`org.nuxeo.ecm.core.test.StorageConfiguration#init()`](https://github.com/nuxeo/nuxeo/blob/master/nuxeo-core/nuxeo-core-test/src/main/java/org/nuxeo/ecm/core/test/StorageConfiguration.java#L120) interprets those properties and makes them available to the Nuxeo Testing Framework.
- with **`<someDB>`** Maven profile activated  
  values: `default`, `pgsql`, `mssql`, `oracle11g`, `oracle12c`, `mysql`, `mongodb`, `mariadb`...
    - some database specific properties are optionally set (database kind, default port...)
    - the Nuxeo Testing Framework instantiates the right `org.nuxeo.ecm.core.storage.sql.DatabaseHelper`

Note that variables in descriptor are resolved when loaded in [DefaultRuntimeContext](org.nuxeo.runtime.model.impl.DefaultRuntimeContext#createRegistrationInfo(org.nuxeo.runtime.model.StreamRef)
Since surefire 2.18 empty properties are propagated which is not expected by some tests that
want to resolve later undefined variables, to do this empty system properties that starts with `nuxeo.test.`
are removed by the test [RuntimeHarness](org.nuxeo.runtime.test.RuntimeHarnessImpl#wipeEmptyTestSystemProperties).

### Properties Conventions

The `vcstests.xml` inherits the properties initialization from `nuxeo-ftest.xml` then it copies the properties to `nuxeo.test.\*`. For instance:

- `nuxeo.db.host` => `nuxeo.test.vcs.server`
- `nuxeo.db.port` => `nuxeo.test.vcs.port`
- `nuxeo.mongodb.server` => `nuxeo.test.mongodb.server`
- `nuxeo.marklogic.host` => `nuxeo.test.marklogic.host`

See https://github.com/nuxeo/tools-nuxeo-ftest#java-parameters for the conventions implemented in `org.nuxeo:nuxeo-ftest`. Roughly here is the logic:

- the database profile is put in `maven.dbprofile` property
- for a given list of database variables (`HOST`, `PORT`, `NAME`, `USER`...),  
  if the global variable `env.NX_DB_@{dbvar}` is not set,  
  then lookup for `env.NX_${maven.dbprofile}_DB_@{dbvar}` specific variable instead.  
  If no variable is set, then defaults to various behaviors (hardcoded default value, dynamic temporary value, error...).


### Sample Ouput

Here are some extracts from a typical execution of `ondemand-testandpush` job against MySQL database.

```
[INFO] --- ant-assembly-maven-plugin:2.1.0:build (setup-vcs) @ nuxeo-ecm ---
[INFO] Active Maven profiles:
qa (source: external)
qa (source: org.nuxeo:nuxeo-ecm:10.3-SNAPSHOT)
customdb (source: org.nuxeo:nuxeo-ecm:10.3-SNAPSHOT)
mysql (source: org.nuxeo:nuxeo-ecm:10.3-SNAPSHOT)

vcs-tests._initdb:
[INFO]      [echo] Using NX_MYSQL_DB_PORT fallback for undefined NX_DB_PORT
[INFO]      [echo] Using NX_MYSQL_DB_NAME fallback for undefined NX_DB_NAME
[INFO]      [echo] Using NX_MYSQL_DB_ADMINNAME fallback for undefined NX_DB_ADMINNAME
[INFO]      [echo] Using NX_MYSQL_DB_ADMINUSER fallback for undefined NX_DB_ADMINUSER
[INFO]      [echo] Using NX_MYSQL_DB_ADMINPASS fallback for undefined NX_DB_ADMINPASS

vcs-tests.vcs-tests-setup:
[INFO] [propertyfile] Updating property file: /opt/jenkins/nuxeo-test-vcs.properties

vcs-tests.prepare-db:
[INFO]      [echo] Prepare MySQL...

vcs-tests.setup-mysql-driver:

vcs-tests.mysql-dbdrop:
[INFO]       [sql] Executing commands
[INFO]       [sql] 4 of 4 SQL statements executed successfully

vcs-tests.mysql-dbcreate:
[INFO]       [sql] Executing commands
[INFO]       [sql] 4 of 4 SQL statements executed successfully

[INFO] --- properties-maven-plugin:1.0-alpha-2:read-project-properties (default) @ nuxeo-ecm ---

[INFO] --- maven-surefire-plugin:2.17:test (default-test) @ nuxeo-core-test ---
[INFO] Surefire report directory: (...)
[INFO] Using configured provider org.apache.maven.surefire.junitcore.JUnitCoreProvider
[INFO] parallel='none', perCoreThreadCount=true, threadCount=0, useUnlimitedThreads=false, threadCountSuites=0, threadCountClasses=0, threadCountMethods=0, parallelOptimized=true
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
StorageConfiguration: Deploying JDBC using DatabaseMySQL
StorageConfiguration: Deploying a VCS repository
(...)
```

## Usage

1) Sample Environment setup

```
    export NX_DB_NAME=testvcs
    export NX_DB_USER=testvcs
    export NX_DB_PASS=testvcs
    export NX_MSSQL_DB_HOST=sqlserver.mydomain.com
    export NX_MSSQL_DB_PORT=1433
    export NX_MSSQL_DB_ADMINNAME=master
    export NX_MSSQL_DB_ADMINUSER=sa
    export NX_MSSQL_DB_ADMINPASS=supersecretpassw0rd
```

2) Run your tests with both the `customdb` profile and the profile corresponding to your database:

    `mvn test -Pcustomdb,mssql`

# Jenkinsfiles

Those files are used by the continuous integration.
The Groovy files are Jenkins Pipeline scripts.

If you need to know the default environment variables set, then you can issue one of:

    docker run --rm dockerpriv.nuxeo.com:443/nuxeo/jenkins-slave /sbin/my_init -- su - jenkins -c "env|sort"

    export TESTS_COMMAND="env|sort"
    docker-compose -f integration/Jenkinsfiles/docker-compose-mongodb-3.4.yml up --no-color --build --abort-on-container-exit tests db
