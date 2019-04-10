# Nuxeo Quota

Nuxeo Quota module. 

## About this module

This module provides a pluggable infrastructure to be able to collect some real time statistics about content in the repository and enforce rules (like Quota).

### Principles

The QuotaStatsService service provides an extension point to register QuotaStatsUpdater that will be responsible for computing the statistics.
The QuotaStatsUpdater will be called :

 - via a Synchrous Event Listener
 - in batch mode for initial computation

The batch mode is triggered via the Admin Center.

### Default contributions

There are 2 default contributions to QuotaStatsService:

#### documentsCountUpdater

Counts the number non folderish objects and maintain the total number on the parent.
Count data is stored in a documents_count_statistics.xsd schema that is automatically added to each folderish document during computation.

#### documentsSizeUpdater

Computes Blob size on each item in the content tree.
Each content item in the tree will have an additional schema documents_size_statistics.xsd that contains :

 - the inner size of the object (innerSize) : size of all the blobs in this document (in bytes)
 - the total size of the object (totalSize) : inner size + size of all the children (in bytes)
 - the allowed maximum total size (maxSize) : maximum quota

If the maxSize is set to -1, then no quota check is performed, otherwise maxSize enforcement is done synchronously.

documentsSizeUpdater works in a 2 phases manner :

 - synchronously : check what has changed in the document and checks quota
 - asynchronously : update counters on item and all parents

The synchronous execution that checks Quota will rollback the transaction and raise a QuotaExceededException if the current transaction would break the quota rule.

The current implementation of documentSizeUpdater :

 - handles checks create / update / delete / move / copy
 - takes into accounts the versions (Document with 2 versions will have a total size of innersize + size of the versions).

## Automation API 

2 Automation Operations are defined to be able to remotely manage the Quota on a given Document :

 - Quotas.GetInfo : to retrieve informations about the Quota Info of a Document (innerSize, totalSize and maxSize)
 - Quotas.SetMaxSize : to define the maximum size allowed in a given Document 

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

