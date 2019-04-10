# Nuxeo User Registration

This addon provides a way to invite external user into Nuxeo, and may add them some permission on a Document.

## Building and deploying

To see the list of all commands available for building and deploying, use the following:

    $ ant usage


### How to build

You can build Nuxeo User Registration with:

    $ ant build

If you want to build and launch the tests, do it with:

    $ ant build-with-tests


### How to deploy

Configure the build.properties files (starting from the `build.properties.sample` file to be found in the current folder), to point your Tomcat instance:

    $ cp build.properties.sample build.properties
    $ vi build.properties

You can then deploy Nuxeo User Registration to your Tomcat instance with:

    $ ant deploy-tomcat


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management] [1] and packaged applications for [document management] [2], [digital asset management] [3] and [case management] [4]. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

[1]: http://www.nuxeo.com/en/products/ep
[2]: http://www.nuxeo.com/en/products/document-management
[3]: http://www.nuxeo.com/en/products/dam
[4]: http://www.nuxeo.com/en/products/case-management

More information on: <http://www.nuxeo.com/>


