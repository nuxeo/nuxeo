Nuxeo Platform Rendition
========================

This addon will provide a RenditionService to register RenditionDefinition with:
* a name
* a label
* an operation chain to use to transform the main Blob

A new document will be created with the Rendition facet, based on a version of the
 given document (the version is created if needed). It will only render
 the main attached file (file:content) and remove all Blobs stored into files:files.

The module nuxeo-platform-rendition-publisher will contribute to the publication
to allow the user to choose to publish a Rendition of the current document.


Building Nuxeo Platform Rendition
=================================

    $ ant build


Deploy Nuxeo Platform Rendition on a Tomcat instance
====================================================

Configure the build.properties files (starting from the
build.properties.sample file to be found in the current folder),
to point your Tomcat instance::

    $ cp build.properties.sample build.properties
    $ vi build.properties

You can then build and deploy Nuxeo Platform Rendition with:

    $ ant deploy
