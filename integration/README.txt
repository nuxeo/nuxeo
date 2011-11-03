Those files are used to set up databases for VCS tests.
The setup is based on tools-nuxeo-ftest and uses the same environment
variables and profiles setup.

Example usage:

1) Environment setup
export NX_DB_NAME=testvcs
export NX_DB_USER=testvcs
export NX_DB_PASS=testvcs
export NX_MSSQL_DB_HOST=sqlserver.mydomain.com
export NX_MSSQL_DB_PORT=1433
export NX_MSSQL_DB_ADMINNAME=master
export NX_MSSQL_DB_ADMINUSER=sa
export NX_MSSQL_DB_ADMINPASS=supersecretpassw0rd

2) Setup db and properties file
mvn -f integration/pom-vcs-setup.xml initialize -Pmssql

3) Run tests normally (this will use the properties file)
mvn test

4) Cleanup
mvn -f integration/pom-vcs-teardown.xml initialize -Pmssql
