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
 
SELECT * FROM Document ecm:mixinType !='HiddenInNavigation' AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState !='deleted'


## Customization
### Change the default query by retrieving your selection of documents (based on different selected facets or types...)
### Use the extension point "exportLogic" to change the logic of export
A contribution "CustomExporterPlugin" has been implemented as an example: provides the default export + create the XML version

# About Nuxeo
Nuxeo provides a modular, extensible Java-based open source software platform for enterprise content management and packaged applications for document management, digital asset management and case management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: http://www.nuxeo.com/
