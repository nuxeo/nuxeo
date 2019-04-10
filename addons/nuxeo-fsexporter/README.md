nuxeo-fsexporter
================

Customizable exporter
# Introduction
This plugin enables the user to export one chosen root structure of documents in Nuxeo to a specified File System repository.

The installation of the plugin will add a new operation in “Services” called “ExportStructureToFS”. This operation must be added to the registries in Studio and then can be used in an automation chain.

## Links
[Blog "Export Your Nuxeo Tree Structure to Your File System"](http://www.nuxeo.com/blog/development/2014/05/export-root-structure-documents-nuxeo-file-system-repository/)

# Usage
## Default Behavior
The operation “ExportStructureToFS” has the 3 parameters:
- Root Name: the root name of the structure of Nuxeo Platform that will be exported
- File System Target: where the export will be done on the File System
- Query: optional parameter. By default the query called by the exporter is:

SELECT * FROM Document ecm:mixinType !='HiddenInNavigation' AND ecm:isVersion = 0 AND ecm:currentLifeCycleState !='deleted'


## Customization
### Change the default query by retrieving your selection of documents (based on different selected facets or types...)
### Use the extension point "exportLogic" to change the logic of export
A contribution "CustomExporterPlugin" has been implemented as an example: provides the default export + create the XML version

# Building

    mvn clean install

## Deploying

Install [the Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/fs-exporter).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

## QA results

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=addons_nuxeo-fsexporter-master)](https://qa.nuxeo.org/jenkins/job/addons_nuxeo-fsexporter-master/)

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
