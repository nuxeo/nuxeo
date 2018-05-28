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

    python ./scripts/inject-arbres.py -d | redis-cli -n 7 --pipe

By default we use the redis database 7.
See `python inject-arbres.py --help` for more information.


### library dataset

The dataset is taken from the "Mairie de Paris" and contains the [list of books that can be loan in Paris Libraries](http://opendata.paris.fr/explore/dataset/tous-les-documents-des-bibliotheques-de-pret/?tab=metas)

To download the file (276MB, 746k docs) and inject the content into redis:

    python scripts/inject-biblio.py -d | redis-cli -n 7 --pipe

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

## Sim00Setup: Setup Simulation

This simulation initialize the environnement and need to be run first, it is idempotent.

- create a bench workspace
- create a common folder
- create a gatling user group
- create all users in this group
- grant write access on bench workspace to the group

## Sim10MassImporter: Use nuxeo-platform-importer

This simulation requires a Nuxeo instance with nuxeo-platform-importer.


## Sim10CreateFolders: Create folder layout

Create the folder layout, using the rest API as listed in Redis `imp:folder`.


## Sim20CreateDocuments: Create documents

Create the documents, using the rest API as listed in Redis `imp:doc`

## Sim25WaitForAsync: Wait for the end of background jobs

After a mass import is done, some jobs may be queued and need to be
exectuted before continuing on other performance tests.

## Sim30UpdateDocuments: Update documents

Update description of a document take document from `imp:temp:doc:created`


## Sim30Navigation: Rest navigation

Get a random folder and document using the REST API (taken from `imp:temp:doc:created`)

## Sim30NavigationJsf: JSF Navigation

View a random folder and a document in it, view all document tabs (doc taken from `imp:temp:doc:created`)


## Sim50Bench: mixing JSF, Rest Navigation and Document update

To setup the proportion you need to prefix the options with:
`nav.` for Rest Navigation
`navjsf.` for JSF Navigation
`upd.`  for Document update

For instance: `-Dnav.users=30 -Dnavjsf=10 -Dupd.user=5 -Dnavjsf.pause_ms=5000`

## Sim80ReindexAll: Run an Elasticsearch reindex all

Drop and recreate the repository index.

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

    mvn -nsu test gatling:test -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim00Setup  -Pbench

Common options with default values:

    # Nuxeo target URL
    -Durl=http://localhost:8080/nuxeo
    # Redis access
    -DredisHost=localhost -DredisPort=6379 -DredisDb=7 -DredisNamespace=imp
    # Number of concurrent users, default depends on simulation
    -Dusers=8
    # Time in second to reach the target number of concurrent users
    -Dramp=0
    # Average pause in millisecond between action, follow an exponential distribution
    -Dpause_ms=0
    # Duration in second of the simulation
    -Dduration=60

Note that you may need to edit the administrator account if it is not the default one:

    src/test/resources/data/admins.csv


# Resources

## Reporting issues

https://jira.nuxeo.com/browse/NXP-17739
https://jira.nuxeo.com/secure/CreateIssue!default.jspa?project=NXP

# Licensing

[GNU Lesser General Public License (LGPL) v2.1](http://www.gnu.org/licenses/lgpl-2.1.html)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).
