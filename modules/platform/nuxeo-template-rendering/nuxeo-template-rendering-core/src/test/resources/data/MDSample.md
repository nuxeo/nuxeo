
# Overview

é è à

## What is this plugin for ?

This Nuxeo Platform plugin provides a way to associate a Nuxeo Document with a Template.

The Template is a Word XML or OpenOffice document (stored also in a Nuxeo Document).

The Template is used to render the Document.

For exemple you can define in your template a field that will be replaced by the dc:title of the document.

Each format (WordML / OpenOffice) has his own logic for defining merge fields.

In the case of OpenOffice document, you can actually generate the full document since it uses JODReports
(that use Freemarker to render an ODT file).

## History

This plugin is based on the DocX prototype done a long time ago here : http://hg.nuxeo.org/sandbox/nuxeo-platform-docx-templates/

## Building

	mvn clean install

## Deploying

You first need to deploy the plugin inside your Nuxeo server.

	cp target/nuxeo-platform-rendering-templates-X.Y.Z.jar into nxserver/bundles or nxserver/plugins

You also need to copy the additional libs (there is no Marketplace package for now)

        cp target/libs/*  nxserver/lib/

## Templating format

The main templating engine is now <A href="http://code.google.com/p/xdocreport/">XDocReport</A> using Freemarker engine.

## Task list

 See the <A href="https://jira.nuxeo.com/browse/NXP-8201">Jira tickets</A>

# Quick User guide

## Template Document

The first step is to create a Template Document.

This document will hold :

 - the template itself (docx or odt for now)

 - the parameters of the template

 - the options for template binding

### Template Parameters

The template file itself uses Freemarker and can contain variable or Freemarker directives.

The default rendering context contains :

 - doc and document : both refering to the DocumentModel the template is rendered against

 - user : the current username

 - principal : the current user principal

 - auditEntries : the Audit entries associated to the DocumentModel the template is rendered against

But of course you can introduce new variables in your template.
For example you can use ${mydescription} in the template file.

When creating a Template Document, the system will automatically try to extract all the variable names used in the template and ask you to provide a binding.

Each variable can be :

 - a String literal

 - a Date literal

 - a Boolean literal

 - a Document Property : a xpath that will be used to extract a property on the target Document

 - a Picture Document Property : same as Document property, but it must point to a Blob that will be used to replace a Picture inside the template file

### Template Binding options

When creating or editing a Template Document, in addition of the template file parameters, you will be able to define options.

Here is a quick description of these options :

#### Applicable Document Types

Because your template can have parameters bound to xpath that are specific to a Document Type, you can define that your template is only available for some document types.

#### Automatic association

You can manually associate a Template Document to any Document in the Content Repository.
If you want that association to be automatic, you can select the Document Types that should benefit from this association.

Typically, if I create a Template Document that is designed for Note and want it to be by default automatically associated to any Note I create, you will select Note doc type in Automatic Association.

#### Template Engine

The templating feature is pluggable so that we can easily add new template processors.
Current implementation comes with 3 built-in processors :

 - XDocReport processor (ODT and DocX)

 - JODReport processor (legacy code for ODT)

 - raw XML processor for WordXML

When creating a Template Document you can select the target processor or keep the default value (automatic) to let the system choose the best processor depending on your template file mime-type.
For now the default will always be XDocReport, but in the future it may change if for example we add raw freemarker or itext support for managing other template types than ODT and DocX).

## Using Template Documents on an other Document

The idea is to use a Template Document to render an other Document.

### TemplateBasedDocument

As example we provide a sample DocumentType that is called TemplateBasedDocument that is basically a File associated to a template.

 - you select the Template associated

 - you can render the template and have the resulting file stored in the file schema.

But this is probably not the main use case.

### Dynamic binding

You dynamically bind a template to an existing Document.
For that on each Document that is not already bound to a Template you will have a new Action on the top right corner: it will allow you to select a Template and associate it to the current Document.
Technicall, we add a facet to the current Document.

NB : Using the automatic association in the Template Document is a way to avoid this manual operation.

### Using the template bound Document

Once your Document is associated to a template (manually or automatically) you will have :

 - a new render action on the top right corner

 - a new Associated Template tab

#### Rendering

It will render the template against the current Document and return you the resulting file.

#### Associated Template tab

This tab provides :

 - a link to the associated Template Document

 - a summary of the template parameters (that you may be able to edit depending on the configuration)

 - a "render to file" action : same as rendering but stores the result as attachement of the current document

 - a "update template parameters" action : will resync the document with the Template Document parameters (useful if you edited the Template Document since the initial association)
