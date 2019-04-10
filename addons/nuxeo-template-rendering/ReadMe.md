
# Overview

## Introduction

This set of plugins provides a way to associate a Nuxeo Document with a Template.

The Template can be of several kind (MS Word XML, OpenOffice text, OpenOffice Calc, MS Excel, Freemarker, XSLT ...)
This Template is stored as a Nuxeo Document and then associated to an other Document.

A given Document can be associated to 0, 1 or several Templates.

The Templates are used to render the associated document.
Depending on the Template type, a different Template Processor will be used and the resulting rendering can be :

 - an HTML document
 - an XML document
 - an OpenOffice document
 - an MS Office document

Each template processor has his own logic for rendering a Document from a Template :

 - raw processing (FreeMarker or XSLT)
 - merge fields replacement (MS Office / OpenOffice)

## Sample use cases 

### Office templating

This is the most direct use case.

**The Template**

The template is an MS Word/Excel or OpenOffice Write/Calc file.

This file can be your company model (with your logo, colors ...), but can also contains merge-fields that will be replaced by the target document data :

 - title
 - version
 - author
 - history
 - picture
 - ...

**The associated document**

The associated document can be any Document that can contain a file.
On first association or at creation time, the main file of the document will be replaced by the template one : it is initialized from the template file.

This means you can continue edit it's content, but this file is dynamic and you can render it to get the final file with all meta-data and presentation stuffs up to date (title, description, author, history ...).

### Web rendering

**The Template**

The Template can for example a FreeMarker template.
It can reference any attribute of the target Document, but also history in order to provide a HTML view of it.

**The associated document**

The associated document can be any Document type and the Freemarker template will be used to provide an HTML view on it 

**Example of URLs**

The URL to access the document with the template applied is the following :

http://**nuxeo\_server\_url**/nxtemplate/**path\_to\_the\_document**@**template\_name**

ex :

http://localhost:8080/nuxeo/nxtemplate/default/default-domain/UserWorkspaces/Administrator/MyDocument@MyTemplate

or for a published document :

http://localhost:8080/nuxeo/nxtemplate/default/default-domain/sections/MySection/MyDocument@MyTemplate

### Composition

**The Template**

The template can be a corporate template with logo, table of content, picture and content in MS Word or OpenOffice format.
The template contains simple merge fields (like in Office templating), but also a *content* field.

**The associated Document**

The associated document can be anything that can be HTML rendered (Note, WebPage, Office document ...).

The rendering will replace the merge fields but also merge the HTML content of the document inside the content field of the template.

This can be used to :

 - render an Html / Markdown note inside an Office Template 
   (i.e. adding a coverpage, a TOC, page numberring ...)

 - fill a mail or form template with formated text

## Templates and Renditions

Template can be used to provide a Rendition.
This means that for example, when you publish a document, you can in fact publish the redering of the document using a template.

# Artifact info

## History

This plugin is based on the DocX prototype done a long time ago here : http://hg.nuxeo.org/sandbox/nuxeo-platform-docx-templates/

The initial rewrite is available here : https://github.com/tiry/nuxeo-platform-rendering-templates

## Sub modules

The project is splitted in several sub modules :

**nuxeo-template-rendering-api**

API module containing all interfaces.

**nuxeo-template-rendering-core**

Component, extension points and service implementation.
This modules only contains template processors for FreeMarker and XSLT.

**nuxeo-template-rendering-web**

Contribute UI level extensions : Layouts, Widgets, Views, Url bindings ...

**nuxeo-template-rendering-xdocreport**

Contribute the OpenOffice / DocX processor based on XDocReport.
This is by far the most powerfull processor.

See : http://code.google.com/p/xdocreport/

**nuxeo-template-rendering-jxls**

Contribute a template processor for XLS files based on JXLS project.
See : http://jxls.sourceforge.net/

**nuxeo-template-rendering-jod**

Contribute JOD Report based template processor for ODT files.
This renderer is historical and replaced by xdocreport that is more powerful.

**nuxeo-template-rendering-jaxrs**

Contribute a JAXRS simple API as well as a new WebTemplate doc type that is based on a Note rather than a file.

**nuxeo-template-rendering-sandbox**

Misc code and extensions that are currently experimental.

**nuxeo-template-rendering-package**

Builder for marketplace package.

## Building

	mvn clean install

## Deploying

You need :

 - to install needed bundles
 - to install needed third paarty libs

You can use the Marketplace package for that.

	

