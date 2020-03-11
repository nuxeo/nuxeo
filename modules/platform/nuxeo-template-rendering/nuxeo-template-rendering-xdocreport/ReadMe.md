This modules contains [XDocReport http://code.google.com/p/xdocreport/] plugins of nuxexo-template-rendering module.

## TemplateProcessor

XDocReportProcessor provides an implemantation of the TemplateProcessor that uses XDocReport as an engine.

## Supported formats

This template processor supports several formats :

 - OpenOffice ODT (Write)
 - OpenOffice ODS (Calc)
 - MS Office docx (Word)

## Supported features

 - merge fields
 - loops on fields
 - picture insertion
 - text formatting and content inclusion

## Templating format

See [XDocReport documentation  http://code.google.com/p/xdocreport/wiki/DesignReport].

## XDocReport Remoting

This module also contains a WebEngine JAX-RS module to provide experimental support for XDocReport remoting and tooling.

## XDocReportTools and REST bindings

XDocReport provide an OpenOffice addon that help the design of Templates.
This is still an early version, but you can however use it with Nuxeo.

The field definition XML file can be generated from Nuxeo Rest API.
The field definition file can be generate on a per document type basis :

 http://server:port/nuxeo/site/xdoctemplates/xdocresources/model/{docType}

For XDocReport Resource remote service you can use :

 http://server:post/nuxeo/site/xdoctemplates/xdocresources
