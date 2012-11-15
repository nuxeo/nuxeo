===========================
Nuxeo Platform Smart Search
===========================

Addon adding UI features to build a query, and save it in a document.

The modules nuxeo-platform-smart-query* are in charge of the query build,
they add a "Smart search" link in the top right corner of every Nuxeo page
that makes it possible to build a query and see its results.

The modules nuxeo-platform-smart-folder-* add features to save the search in
a document model of type "smart folder". This document type can also be
created as other documents. It presents the search results on its view tab,
and a form similar to the smart search form on its edit tab and creation
page.

Install
-------

Setup properties in a file called "build.properties" according to your needs
and run "ant deploy" to deploy on a jboss with nuxeo installed.

This is equivalent to copying the jars to the director
$JBOSS_HOME/server/default/deploy/nuxeo.ear/plugins.
