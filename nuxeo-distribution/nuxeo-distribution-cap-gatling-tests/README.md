# Nuxeo Gatling bench

# Requirement

- A running Nuxeo server instance
- A Redis server (>= 2.4.14 for the `--pipe` support) by default expect to run on `localhost:6379`
- Setup an existing administrator account in the file: `src/test/resources/data/admins.csv`
- Setup a list of users that will be created as members and used in the bench: `src/test/resources/data/users.csv`
  default file contains 500 users.
- Increase vcs and db pool size accordingly to the concurrent requests expected (in `nuxeo.conf`: `nuxeo.*
.max-pool-size`)


# Overview

Gatling is used to execute simulations (aka scenario).

The Setup simulation creates a bench workspace and users taken from a CSV file.

Then there are simulations to create documents using the Nuxeo Rest API. Documents are served by a Redis server. The
folder layout is created first then the leaf documents.

It is easy to inject documents into Redis using provided script and open data or by creating custom script.

Once documents are loaded, simulation of UI (JSF) navigation can be run, it is possible to mix navigation
with document update.


# Injecting documents into Redis

## From open data

There a few sample provided

### Tree dataset

The dataset is taken from the "Mairie de Paris" and contains the [list of tree in the Paris area](http://opendata.paris.fr/explore/dataset/les-arbres/?tab=metas)

To download the file (12MB, 90k docs) and inject the content into redis:

    python inject-arbre.py | redis-cli --pipe

See `python inject-arbre.py --help` for more information, note that the file can be processed much faster using GNU parallel:

    cat ~/data/les-arbres.csv | parallel --pipe --block 2M python ./inject-arbres.py | redis-cli --pipe


### library dataset

The dataset is taken from the "Mairie de Paris" and contains the [list of books that can be loan in Paris Libraries](http://opendata.paris.fr/explore/dataset/tous-les-documents-des-bibliotheques-de-pret/?tab=metas)

To download the file (276MB, 746k docs) and inject the content into redis:

    python inject-biblio.py | redis-cli --pipe


## From a custom python script

Create document using the `NuxeoWriter.addDocument` API, the folders layout will be build automatically:

      from nuxeo import NuxeoWriter

      writer = NuxeoWriter(RedisWriter(usePipeProtocol=True))
      # create a doc using: name, docType, parentPath and properties
      writer.addDocument("mydoc", "File", "some/path/to/create, {"dc:title": "A document file"})


This will output [Redis pipe protocol](http://redis.io/topics/mass-insert) that can be read by the `redis-cli` like
this:

      python  my-script.py | redis-cli --pipe


See the previous `inject-*.py` file for example.

## Redis document store

The documents information are stored in a Redis database with the following layout:

- `imp:folder` a ZSET of folders path and level
- `imp:doc` a SET of documents path
- `imp:data:$docPath` a HASH describing a document (or a folder) including the payload that will be used to create
the Nuxeo document

When running the gatling tests it will also produces:

- `imp:temp:doc:toCreate` a SET of documents waiting to be created
- `imp:temp:doc:creating` a SET of documents in creation or in failure after the import
- `imp:temp:doc:created` a SET of documents that have been created

Some useful `redit-cli` commands:

    # Number of document to create
    SCARD imp:doc
    # Number of document to create
    ZCARD imp:temp:folder:toCreate
    # Number of document in creation or failed after import
    SCARD imp:temp:folder:creating
    # Number of document created
    SCARD imp:temp:folder:created
    # View a doc
    HGETALL imp:data:<docPath>


# Simulations

## Setup Simulation

This simulation initialize the environnement and need to be run first, it is idempotent.

- create a bench workspace
- create a common folder
- create a gatling user group
- create all users in this group
- grant write access on bench workspace to the group

## Create folder layout

Create the folder layout, using the rest API as listed in Redis `imp:folder`.


## Create documents

Create the documents, using the rest API as listed in Redis `imp:doc`

Options:

    # number of concurrent users creating documents
    -Dusers=8

## Update documents

Update description of a document take document from `imp:temp:doc:created`

Options:

    # number of concurrent users creating documents
    -Dusers=8
    # duration in second of the bench
    -Dduration=30
    # user thinktime in second between update
    -DthinkTime=0

## Navigation

View random folder and document tabs, taken from `imp:temp:doc:created`

Options:

    # number of concurrent users
    -Dusers=8
    # duration in second of the bench
    -Dduration=30
    # user thinktime in second between update
    -DthinkTime=0

## BenchSimulation (mixing simulations)

x% UpdateDocument
y% Navigation

## Cleanup simulation

This simulation remove all documents, users and group from the Nuxeo instance, also delete the data in redis.

# Launching

## Setup a Nuxeo instance and run all simulations

This will import a default data set, start a server and run the following simulations:
- Setup
- CreateFolders 
- CreateDocuments
- UpdateDocuments
- Navigation
- Cleanup


    mvn -nsu integration-test -Pbench


Default options: see below

## Running a single simulation on an existing instance

    mvn -nsu test gatling:execute -Dgatling.simulationClass -Pbench
    ...
    Choose a simulation number:
     [0] org.nuxeo.cap.bench.Sim00Setup
     [1] org.nuxeo.cap.bench.Sim10CreateFolders
     [2] org.nuxeo.cap.bench.Sim20CreateDocuments
     [3] org.nuxeo.cap.bench.Sim30Navigation
     [4] org.nuxeo.cap.bench.Sim30UpdateDocuments
     [5] org.nuxeo.cap.bench.Sim50Bench
     [6] org.nuxeo.cap.bench.Sim90Cleanup
     


Common options with default values:

    # Nuxeo target URL
    -Durl=http://localhost:8080/nuxeo
    # Redis access
    -DredisHost=localhost -DredisPort=6379
    # Redis key prefix (or namespace)
    -DredisNamespace=imp


You can also bypass the interactive mode and execute a simulation

    mvn -nsu test gatling:execute -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim00Setup -Pbench
