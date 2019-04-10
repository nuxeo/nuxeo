# Nuxeo Quota

Nuxeo Quota module.

## About this module

This module provides a pluggable infrastructure to be able to collect some real time statistics about content in the repository and enforce rules (like Quota).

### Principles

The QuotaStatsService service provides an extension point to register QuotaStatsUpdater that will be responsible for computing the statistics.
The QuotaStatsUpdater will be called:

 - via a Synchronous Event Listener
 - in batch mode for initial computation

The batch mode is triggered via the Admin Center.

### Default contributions

There are 2 default contributions to QuotaStatsService:

#### documentsCountUpdater

Counts the number non folderish objects and maintain the total number on the parent.
Count data is stored in a documents_count_statistics.xsd schema that is automatically added to each folderish document during computation.

#### documentsSizeUpdater

Computes Blob size on each item in the content tree.
Each content item in the tree will have an additional schema documents_size_statistics.xsd that contains:

 - the inner size of the object (innerSize): size of all the blobs in this document (in bytes)
 - the total size of the object (totalSize): inner size + size of all the children (in bytes)
 - the allowed maximum total size (maxSize): maximum quota

If the maxSize is set to -1, then no quota check is performed, otherwise maxSize enforcement is done synchronously.

documentsSizeUpdater works in a 2 phases manner:

 - synchronously : check what has changed in the document and checks quota
 - asynchronously : update counters on item and all parents

The synchronous execution that checks Quota will rollback the transaction and raise a QuotaExceededException if the current transaction would break the quota rule.

The current implementation of documentSizeUpdater:

 - handles checks create / update / delete / move / copy
 - takes into accounts the versions (Document with 2 versions will have a total size of inner size + size of the versions).

## Automation API

2 Automation Operations are defined to be able to remotely manage the Quota on a given Document:

 - Quotas.GetInfo: to retrieve informations about the Quota Info of a Document (innerSize, totalSize and maxSize)
 - Quotas.SetMaxSize: to define the maximum size allowed in a given Document

# Building

    mvn clean install

## Deploying

Install [the Nuxeo Quota Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-quota).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-quota-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-quota-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
