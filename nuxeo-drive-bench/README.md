# Nuxeo Drive bench

# Requirement

- A running Nuxeo server instance with Nuxeo Drive marketplace package installed
- A redis server running on `localhost:6379`
- Setup an existing administrator account in the file: `src/test/resources/data/admins.csv`
- Setup a list of users that will be created as members and used in the bench: `src/test/resources/data/users.csv`
  default file contains 500 users.
- Increase vcs and db pool size to 60 in `nuxeo.conf`: `nuxeo.*.max-pool-size`

# Scenario

## Setup

    mvn -o test
    ...
    Choose simulation: org.nuxeo.drive.bench.SetupSimulation


This simulation initialize the environnement and need to be run first, it is idempotent.

- create a bench workspace
- create a common folder
- create a gatling user group
- create all users in this group
- create a folder for each user in the bench workspace
- synchronize the common and user folder for each user
- save user information (user, token, deviceId) in redis


## DriveBench

    mvn -o test -DrampUp=10 -Dduration=120
    ...
    Choose simulation: org.nuxeo.drive.bench.DriveBenchSimulation


This simulation run a mix of different scenarios:

- Polling: simulate 100 drive client polling for update
- Server feeder: simulate 10 writers creating/updating documents in Nuxeo
- TODO: Client feeder: simulate n writers creating/updating documents from drive


### Polling

Simulate the nuxeo drive poll behavor:

- peek a random user in redis
- loop: poll for update every 30s
  - TODO: if there is some changes download

default number of concurrent users: 100, can be tuned with `-Dusers`
default pause between poll 30, can be tuned with `-DpollInterval`

### Server feeder

Simulate document creation on the server side:

- peek a random user
- create a file in the common folder
- create a file in the user folder
- loop: update both file content
- delete both file

default thinktime is 5s between operation, can be tuned with `-DfeederInterval`
default number of concurrent users: 10, can be tuned with `-Dwriters`

### TODO Client feeder

Simulate document creation on the Nuxeo Drive side

- using a random user upload a new document in its folder

## Cleanup

    mvn -o test
    ...
    Choose simulation: org.nuxeo.drive.bench.CleanupSimulation


This simulation remove all documents, users and group from Nuxeo and delete the data in redis.
