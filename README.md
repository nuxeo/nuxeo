# Nuxeo Platform Suggest Box

This repo hosts the source code of a Nuxeo service to suggest actions
(e.g. navigate to a document or user profile page or perform a filtered
search for documents) from a contextual user text input (e.g. search box).

This repo also provides a Seam component and JSF template to override
the top right search box of the default Nuxeo web interface to provide
ajax suggestions.

In the future the service suggestion feature could also be exposed as
a Nuxeo Content Automation operation to build similar UI for clients
using the Android or iOS SDKs for instance.


## Building and deploying

Install Nuxeo 5.5 from source (unreleased yet, use the dev version) and maven
2.2.1+ and run:

    mvn install && cp */target/*-SNAPSHOT.jar $NUXEO_HOME/nxserver/bundles/

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
