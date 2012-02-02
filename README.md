# Nuxeo Diff

This repo hosts the source code of a plugin for Nuxeo Platform that allows to render a diff between two documents or two versions of a document.
The comparison takes into account all the metadatas shared by the documents, which means that if a comparison is done between two documents of a different type, only the schemas in common will be "diffed". 
The result of the comparison only shows the metadatas that have been updated, or added / deleted in the case of multivalued metadatas.
The comparison does not take into account blob-type metadatas.


## Building and deploying

Install Nuxeo 5.6-SNAPSHOT from source and maven 2.2.1+ and run:

    mvn install && cp */target/*-5.6-SNAPSHOT.jar $NUXEO_HOME/nxserver/plugins/

and restart Nuxeo.

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software
platform for enterprise content management] [5] and packaged applications
for [document management] [6], [digital asset management] [7] and
[case management] [8]. Designed by developers for developers, the Nuxeo
platform offers a modern architecture, a powerful plug-in model and
extensive packaging capabilities for building content applications.

[5]: http://www.nuxeo.com/en/products/ep
[6]: http://www.nuxeo.com/en/products/document-management
[7]: http://www.nuxeo.com/en/products/dam
[8]: http://www.nuxeo.com/en/products/case-management

More information on: <http://www.nuxeo.com/>