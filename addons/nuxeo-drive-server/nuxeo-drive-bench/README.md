# Nuxeo Drive bench

# Requirement

- A running Nuxeo server instance with Nuxeo Drive marketplace package installed
- A redis server running on `localhost:6379`
- Setup an existing administrator account in the file: `src/test/resources/data/admins.csv`
- Setup a list of users that will be created as members and used in the bench: `src/test/resources/data/users.csv`
  default file contains 500 users.
- Increase vcs and db pool size to 60 in `nuxeo.conf`: `nuxeo.*.max-pool-size`

# Simulations

## Setup Simulation

This simulation initialize the environnement and need to be run first, it is idempotent.

- create a bench workspace
- create a common folder
- create a gatling user group
- create all users in this group
- create a folder for each user in the bench workspace
- synchronize the common and user folder for each user
- save user information (user, token, deviceId) into redis


## DriveBench Simulation

This simulation run a mix of different scenarios:


### Polling scenario

Simulate the nuxeo drive poll behavor:

- peek a random drive client from redis
- loop: poll for update every 30s
  - TODO: if there is some changes download


### Server feeder scenario

Simulate document creation on the server side:

- peek a random Nuxeo user
- create a file in the common folder
- create a file in the user folder
- loop 2 times
   - update both file content
- delete both file


### TODO Client feeder scenario

Simulate document creation on the Nuxeo Drive side

- using a random drive client from redis
- upload a new document in its folder

## Cleanup simulation

This simulation remove all documents, users and group from the Nuxeo instance, also delete the data in redis.

# Launching

## Run all simulations

    mvn integration-test

This will run simulations: Setup, DriveBench and Cleanup

Default options: see below

## Run Setup simulation 

    mvn gatling:execute -Dgatling.simulationClass=org.nuxeo.drive.bench.SetupSimulation

Default options:

    # Target URL
    -Durl=http://localhost:8080/nuxeo

## Run DriveBench simulation

    mvn gatling:execute -Dgatling.simulationClass=org.nuxeo.drive.bench.DriveBenchSimulation

Default options:

    # Target URL
    -Durl=http://localhost:8080/nuxeo
    # Duration of the bench
    -Dduration=60
    # Concurrent drive clients
    -Dusers=100
    # Concurrent writer using Nuxeo
    -Dwriters=10
    # Sleep time during drive poll
    -DpollInterval=30
    # Sleep time between document creation
    -DfeederInterval=10

## Run Cleanup simulation

    mvn gatling:execute -Dgatling.simulationClass=org.nuxeo.drive.bench.CleanupSimulation

Default options:

    # Target URL
    -Durl=http://localhost:8080/nuxeo

