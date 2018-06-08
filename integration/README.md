# VCS Tests

Database setup for VCS tests.

The setup is based on `tools-nuxeo-ftest` and uses the same environment variables and profiles setup.

1) Environment setup (example)

    export NX_DB_NAME=testvcs
    export NX_DB_USER=testvcs
    export NX_DB_PASS=testvcs
    export NX_MSSQL_DB_HOST=sqlserver.mydomain.com
    export NX_MSSQL_DB_PORT=1433
    export NX_MSSQL_DB_ADMINNAME=master
    export NX_MSSQL_DB_ADMINUSER=sa
    export NX_MSSQL_DB_ADMINPASS=supersecretpassw0rd

2) Run your tests with both the "customdb" profile and the profile corresponding to your database:

    mvn test -Pcustomdb,mssql

# Jenkinsfiles

Those files are used by the continuous integration.
The Groovy files are Jenkins Pipeline scripts.

If you need to know the default environment variables set, then you can issue one of:

    docker run --rm dockerpriv.nuxeo.com:443/nuxeo/jenkins-slave /sbin/my_init -- su - jenkins -c "env|sort"

    export TESTS_COMMAND="env|sort"
    docker-compose -f integration/Jenkinsfiles/docker-compose-mongodb-3.4.yml up --no-color --build --abort-on-container-exit tests db

## Build Status Test and Push (ondemand.groovy)

1.1° Clone	- Clone or update the Nuxeo Platform source code to the master branch, including all addons and Nuxeo

1.2° Rebase	- Used to rebase all branchs compared to the master branch (fallback on master if branch doesn't exist)

1.3° Compile	- Build the Nuxeo Platform `mvn -nsu -B test-compile -Pqa,addons,distrib -DskipTests`

1.4° Test	- Test the compiled source code

1.5° Verify	- Run checks on integration tests

2.1° Postgresql	- Build the Nuxeo Platform with pgsql database started by docker-compose

2.2° MongoDB 	- Build the Nuxeo Platform with mongodb database started by docker-compose 

2.3° Kafka 	- Build the Nuxeo Platform with kafka database started by docker-compose 


