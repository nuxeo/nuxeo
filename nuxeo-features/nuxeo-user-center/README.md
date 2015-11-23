# Nuxeo User Center

This addon adds a Home for every user.

## Building

Using maven 2.2.1 or later, from root folder:

$ mvn clean install

Then copy the built jars **/target/nuxeo-*-SNAPSHOT.jar into the nxserver/bundles folder of your Nuxeo DM (assuming the default tomcat package).

You should then have 'My Personal Space' link that will take you to your new Home :-)


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management] [1] and packaged applications for [document management] [2], [digital asset management] [3] and [case management] [4]. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications. 

[1]: http://www.nuxeo.com/en/products/ep
[2]: http://www.nuxeo.com/en/products/document-management
[3]: http://www.nuxeo.com/en/products/dam
[4]: http://www.nuxeo.com/en/products/case-management

More information on: <http://www.nuxeo.com/>

