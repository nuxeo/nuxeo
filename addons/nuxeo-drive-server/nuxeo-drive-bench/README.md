# Nuxeo Drive Gatling Bench

## Requirements

- A running Nuxeo server instance with the `nuxeo-platform-importer` and `nuxep-drive` packages installed
- A Redis server running on `localhost:6379`
- Setup an existing administrator account in the file: `src/test/resources/data/admins.csv`
- Setup a list of users that will be created as members and used in the bench: `src/test/resources/data/users.csv`.
  Default file contains 500 users.
- Increase vcs and db pool size to 60 in `nuxeo.conf`: `nuxeo.*.max-pool-size`

## Simulations

### Polling Setup Simulation

This simulation initializes the environment for the Polling scenario and needs to be run first, it is idempotent.

- Create a bench workspace
- Create a common folder
- Create a Gatling user group
- Create all users in this group
- Create a folder for each user in the bench workspace
- Synchronize the common and user folder for each user
- Save user information (user, token, deviceId) into Redis

### Polling Simulation

This simulation runs a mix of different scenarios.

#### Polling scenario

Simulates the Nuxeo Drive polling behavior:

- Peek a random Nuxeo Drive client from Redis
- Loop: poll for update every 30s
  - TODO: if there is some changes download

#### Server Feeder Scenario

Simulates document creation on the server side:

- Peek a random Nuxeo user
- Create a file in the common folder
- Create a file in the user folder
- Loop 2 times
   - Update both file contents
- Delete both files

#### TODO: Client Feeder Scenario

Simulates document creation on the Nuxeo Drive side:

- Using a random Nuxeo Drive client from Redis
- Upload a new document in its folder

### Polling Cleanup Simulation

This simulation removes all documents, users and group from the Nuxeo instance, also deletes the data in Redis.

### Remote Scan Setup Simulation

This simulation initializes the environment for the Remote Scan scenario and needs to be run first, it is idempotent.

- Create a Gatling user group
- Create a bench workspace
- Grant ReadWrite permission to the Gatling group on the bench workspace
- Mass import of random documents in the bench workspace
- Wait for asynchronous jobs
- Create users in the Gatling group
- Synchronize the bench workspace for each user
- Save user information (user, token, deviceId) into Redis
- Fetch Automation API
- Get client update info

### Recursive Remote Scan Simulation

Simulates the Nuxeo Drive recursive remote scan behavior:

- Peek a random Nuxeo Drive client from Redis
- Fetch Automation API
- Get top level folder
- Get file system item
- Get initial change summary
- Get children recursively

### Batched Remote Scan Simulation

Simulates the Nuxeo Drive batched remote scan behavior:

- Peek a random Nuxeo Drive client from Redis
- Fetch Automation API
- Get top level folder
- Get file system item
- Get initial change summary
- Get top level folder children: the synchronization roots
- For each synchronization root get descendants by batch

### Remote Scan Cleanup Simulation

This simulation removes all documents, users and group from the Nuxeo instance, also deletes the data in Redis.

## Launching Simulations

### All in One

Sets up a Nuxeo instance with the required packages and configuration, runs all the simulations and stops the Nuxeo instance.

    mvn -nsu verify -Ppolling,remote-scan

You can use a single profile for running a specific group of simulations: `polling`, `remote-scan` or `cleanup-remote-scan`.

You can add the following profiles:

- `pgsql`: use a PostgreSQL database as a backend for Nuxeo
- `monitor`: record metrics to Graphite, see this [sample](http://kraken.nuxeo.com/dashboard/#kraken-drive-remote-scan) for instance

Default options: see below.

### Running a Single Simulation on an Running Nuxeo Instance

    mvn -nsu gatling:test -Dgatling.simulationClass
    ...
    Choose a simulation number:
         [0] org.nuxeo.drive.bench.Sim00SetupPolling
         [1] org.nuxeo.drive.bench.Sim10BenchPolling
         [2] org.nuxeo.drive.bench.Sim20CleanupPolling
         [3] org.nuxeo.drive.bench.Sim30SetupRemoteScan
         [4] org.nuxeo.drive.bench.Sim40BenchRecursiveRemoteScan
         [5] org.nuxeo.drive.bench.Sim45BenchBatchedRemoteScan
         [6] org.nuxeo.drive.bench.Sim50CleanupRemoteScan

Common options with default values:

    # Nuxeo target URL
    -Durl=http://localhost:8080/nuxeo
    # Time in seconds to reach the target number of concurrent Nuxeo Drive clients / writers
    -Dramp=10

Options for the Polling simulations:

    # Number of concurrent Nuxeo Drive clients
    -Dusers=100
    # Number of concurrent writers using Nuxeo
    -Dwriters=10
    # Sleep time in seconds during Nuxeo Drive polling
    -DpollInterval=30
    # Sleep time in seconds between document creations
    -DfeederInterval=10
    # Duration in seconds of the simulation
    -Dduration=60

Options for the Remote Scan simulations:

    # Number of nodes to create during mass import at setup
    -DnbNodes=100000
    # Number of threads to use during mass import at setup
    -DnbThreads=12
    # Number of concurrent Nuxeo Drive clients
    -DremoteScan.users=10
    # Batch size for the batched remote scan
    -DbatchSize=100
    # Sleep time in milliseconds between batch calls
    -DremoteScan.pauseMs=100

Note that you may need to edit the administrator account if it is not the default one:

    src/test/resources/data/admins.csv

You can also bypass the interactive mode and execute a given simulation:

    mvn -nsu gatling:test -Dgatling.simulationClass=org.nuxeo.drive.bench.Sim00SetupPolling

## Resources

### Reporting issues

[https://jira.nuxeo.com/browse/NXDRIVE-367](https://jira.nuxeo.com/browse/NXDRIVE-367)

[https://jira.nuxeo.com/browse/NXP-19209](https://jira.nuxeo.com/browse/NXP-19209)

[https://jira.nuxeo.com/secure/CreateIssue!default.jspa?project=NXP](https://jira.nuxeo.com/secure/CreateIssue!default.jspa?project=NXP)

## Licensing

[Apache License, Version 2.0 (the "License")](http://www.apache.org/licenses/LICENSE-2.0)
