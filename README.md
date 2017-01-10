nuxeo-tree-snapshot
===================

# About

This Nuxeo Platform Addon provides tree snapshoting feature (i.e. folder versioning).

Default repository versioning model only supports leaf versioning, so if you need a version (snapshot) a complete tree this addon can help you.

# What it does

Here is a typical use case example (actually, this is the output of the unit test in verbose mode) :

# Building

    mvn clean install

## Deploying

Copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/addons_nuxeo-tree-snapshot-master)](https://qa.nuxeo.org/jenkins/job/master/job/addons_nuxeo-tree-snapshot-master/)

## Initial Tree

Dumping Live docs in repository

    /root -  -- root
    /root/folder1 -  -- Folder 1
    /root/folder1/doc12 - 0.0 -- Doc 12
    /root/folder1/folder11 -  -- Folder 11
    /root/folder1/folder13 -  -- Folder 13
    /root/folder1/folder13/folder131 -  -- Folder 131
    /root/folder1/folder13/folder131/doc1311 - 0.1 -- Doc 1311
    /root/folder1/folder13/folder131/doc1312 - 0.1+ -- Doc 1312
    /root/folder2 -  -- Folder 2


## Initial Tree Snapshot

    root -- 0.1
     folder2 -- 0.1
     folder1 -- 0.1
      folder11 -- 0.1
      folder13 -- 0.1
       folder131 -- 0.1
        doc1312 -- 0.2
        doc1311 -- 0.1
      doc12 -- 0.1

## new Tree after updating a doc

Dumping Live docs in repository

    /root - 0.1 -- root
    /root/folder1 - 0.1 -- Folder 1
    /root/folder1/doc12 - 0.1 -- Doc 12
    /root/folder1/folder11 - 0.1 -- Folder 11
    /root/folder1/folder13 - 0.1 -- Folder 13
    /root/folder1/folder13/folder131 - 0.1 -- Folder 131
    /root/folder1/folder13/folder131/doc1311 - 0.1+ -- Doc 1311
    /root/folder1/folder13/folder131/doc1312 - 0.2 -- Doc 1312
    /root/folder2 - 0.1 -- Folder 2


## new Snapshot of the tree

    root -- 0.2
     folder2 -- 0.1
     folder1 -- 0.2
      folder11 -- 0.1
      folder13 -- 0.2
       folder131 -- 0.2
        doc1312 -- 0.2
        doc1311 -- 0.2
      doc12 -- 0.1

## new Tree after cutting a branch

Dumping Live docs in repository

    /root - 0.2 -- root
    /root/folder1 - 0.2 -- Folder 1
    /root/folder1/doc12 - 0.1 -- Doc 12
    /root/folder1/folder11 - 0.1 -- Folder 11
    /root/folder2 - 0.1 -- Folder 2


## new Snapshot of the tree

    root -- 0.3
     folder2 -- 0.1
     folder1 -- 0.3
      folder11 -- 0.1
      doc12 -- 0.1

## new Tree after restore on version 0.2

Dumping Live docs in repository

    /root - 0.2 -- root
    /root/folder1 - 0.2 -- Folder 1
    /root/folder1/doc12 - 0.1 -- Doc 12
    /root/folder1/folder11 - 0.1 -- Folder 11
    /root/folder1/folder13 - 0.2 -- Folder 13
    /root/folder1/folder13/folder131 - 0.2 -- Folder 131
    /root/folder1/folder13/folder131/doc-1311 - 0.2 -- Doc 1311
    /root/folder1/folder13/folder131/doc-1312 - 0.2 -- Doc 1312
    /root/folder2 - 0.1 -- Folder 2

# Using the Addon API

## Simple versioning

The API is provided by an Aadapter system.
If myfolder is a folderish DocumentModel that you want to snapshot, the code should look like :

    Snapshotable snapshotable = myfolder.getAdapter(Snapshotable.class);
    Snapshot snapshot = snapshotable.createSnapshot(VersioningOption.MINOR);

In order to be seen as "Snapshotable", your folderish object must have the facet **Snapshotable** :

    <doctype name="SnapshotableFolder" extends="Folder">
      <facet name="Snapshotable"/>
    </doctype>

## Publishing

This addons also provides a contribution to the publisher service so that you can publish a tree via the publisher.

## Proxy handling

TODO This addon also provides helpers for handling proxies :
* adding a document to a proxy folder actually adds it to its target folder, see TestSnapshotingAndProxies.testAddAndGetProxyFolderChildren()

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
