# Nuxeo Datadog Reporter


## General information and motivation

This component sends the internal Nuxeo metrics to Datadog.


## Getting started


Preferred method is to use the MP package and configure the `datadog.apikey` in your `nuxeo.conf`, that's it !

If you want to compile yourself :

    mvn clean install
    cp target/nuxeo-datadog-reporter*.jar $NUXEO_HOME/nxserver/bundles/

Then add the following line in your `nuxeo.conf` :

    datadog.apikey=MY_DATADOG_API_KEY


##About
###Nuxeo

[Nuxeo](http://www.nuxeo.com) provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/content-management-platform), and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/digital-asset-management), [social collaboration](http://www.nuxeo.com/en/products/social-collaboration) and [case management](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>