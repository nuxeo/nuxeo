# Nuxeo Gatling bench


# Overview

This module provides [Gatling](http://gatling.io/) bench scripts and make it easy to create new one.

The documents are feeded from Redis (see section below), bench users from a CSV file.

The Nuxeo instance is first setup using a simulation (aka scenario) that creates a sandbox workspace
and users.

The next step is to create documents using the Nuxeo Rest API. The folder layout is created first then the leaf
documents.

Once documents are loaded, simulation that update documents or navigate using the JSF UI can be
run.


# Requirement

- A Redis server (>= 2.4.14 for the `--pipe` support) by default expect to run on `localhost:6379`
- The data injection must be done on unix

# Injecting documents into Redis

The goal is to do the document pre processing using custom script, so Gatling has ready to use data serverd
quickly by Redis, it also enable to use multiple Gatling instance.


## Data injectors

### Default "Trees" dataset

The dataset is taken from the "Mairie de Paris" and contains the [list of trees in the Paris area](http://opendata.paris.fr/explore/dataset/les-arbres/?tab=metas)

This data will be download and injected automatically when using the default maven integration-test phase.

You can also run the script by hand, this will download the file (12MB, 90k docs) and inject the content into redis:

    python ./scripts/inject-arbre.py | redis-cli -n 7 --pipe

By default we use the redis database 7.
See `python inject-arbre.py --help` for more information.


### library dataset

The dataset is taken from the "Mairie de Paris" and contains the [list of books that can be loan in Paris Libraries](http://opendata.paris.fr/explore/dataset/tous-les-documents-des-bibliotheques-de-pret/?tab=metas)

To download the file (276MB, 746k docs) and inject the content into redis:

    python scripts/inject-biblio.py | redis-cli -n 7 --pipe

Note that the file can be processed much faster using GNU parallel:

    cat ~/data/biblio.csv | parallel --pipe --block 40M python .scripts/inject-biblio.py | redis-cli -n 7 --pipe

### Custom dataset

Create a python script and use the `NuxeoWriter.addDocument` method, the writer will take care of creating parent
folders if they don't already exists:

      from nuxeo import NuxeoWriter

      writer = NuxeoWriter(RedisWriter(usePipeProtocol=True))
      # create a doc using: name, docType, parentPath and properties
      writer.addDocument("mydoc", "File", "some/path/to/create, {"dc:title": "A document file"})


This will output [Redis pipe protocol](http://redis.io/topics/mass-insert) that can be read by the `redis-cli` like
this:

      python  my-script.py | redis-cli -n 7 --pipe


Create your injector by looking at provided one.

## Redis document store

The documents information are stored in a Redis database with the following layout:

- `imp:folder` a ZSET to list folder with their depth level
- `imp:doc` a SET to list the documents path
- `imp:data:$docPath` a HASH describing a document (or a folder) including the payload that will be used to create
the Nuxeo document

When running the gatling tests it will also produces:

- `imp:temp:doc:toCreate` a SET of documents waiting to be created
- `imp:temp:doc:creating` a SET of documents in creation or in failure after the import
- `imp:temp:doc:created` a SET of documents that have been created

Some useful `redit-cli` commands:

    # Number of document in the data set
    SCARD imp:doc
    # Number of document pending in creation
    ZCARD imp:temp:doc:toCreate
    # Number of document in creation or failed after import
    SCARD imp:temp:doc:creating
    # Number of document created
    SCARD imp:temp:doc:created
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

## Bench (mixing Navigation and Update)

10% UpdateDocument
90% Navigation

## Cleanup simulation

This simulation remove all documents, users and group from the Nuxeo instance, also delete the data in redis.

# Executing bench

To run a gatling test you need to use explicitely the `bench` maven profile.
There is also a `perf` profile that can be added to start with a tuned the Nuxeo instance.


## All in one

Setup a Nuxeo instance, get data, inject data into Redis, run all the simulations and stop Nuxeo.
WARNING this will flush (ERASE) the Redis database 7.

    mvn -nsu verify -Pbench

You can use `integration-test` instead of verify to keep the Nuxeo instance running.


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
    -DredisHost=localhost -DredisPort=6379 -DredisDb=7 -DredisNamespace=imp

Note that you may need to edit the administrator account if it is not the default one:

    src/test/resources/data/admins.csv

You can also bypass the interactive mode and execute a simulation

    mvn -nsu test gatling:execute -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim00Setup -Pbench
