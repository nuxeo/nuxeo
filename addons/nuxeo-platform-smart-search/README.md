# Nuxeo Platform Smart Search

Addon adding UI features to build a query, and save it in a document.

The modules nuxeo-platform-smart-query* are in charge of the query build,
they add a "Smart search" item on the main Search tab default searches,
that makes it possible to build a query and see its results.

The modules nuxeo-platform-smart-folder-* add features to save the search in
a document model of type "smart folder". This document type can also be
created as other documents. It presents the search results on its view tab,
and a form similar to the smart search form on its edit tab and creation
page.

Status: Deprecated since LTS 2016 

Unit Tests: [![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-platform-smart-search-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-platform-smart-search-master/)

Webdriver Tests: [![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_FT_nuxeo-platform-smart-search-master)](https://qa.nuxeo.org/jenkins/job/addons_FT_nuxeo-platform-smart-search-master/)

## Building

    mvn clean install

## Deploying

1. Put generated jars to your nxserver/bundles directory
2. Start the server.

(or just install the corresponding Nuxeo Package)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
