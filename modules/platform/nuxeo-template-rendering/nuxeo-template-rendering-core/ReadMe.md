This modules contains the implementation of nuxexo-template-rendering module.

Main objects are

## TemplateProcessorComponent

Runtime Component used to handle Extension Points and expose the TemplateProcessorService interface
(interface that is used to manipulate TemplateProcessors and associated documents).

## Default TemplateProcessor implementation

 - FreeMarker TemplateProcessor : FreeMarkerProcessor
 - XSLT based TemplateProcessor : XSLTProcessor

## TemplateBasedDocumentAdapterImpl

Default implementation of the DocumentModel adapter for interface TemplateBasedDocument
(a DocumentModel that is bound to one or more templates)

## TemplateSourceDocumentAdapterImpl

Default implementation of the DocumentModel adapter for interface TemplateSourceDocument
(a DocumentModel that can provide a template)

## TemplateBasedRenditionProvider

Provides Rendition based on the template system.

## Automation Operation

Automation Operations to wrapp TemplateProcessorService.
