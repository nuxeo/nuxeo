# nuxeo-template-rendering-jaxrs

## Overview

This module mainly contribute :

 - a JAXRS API for accessing template and template based document and their resources

 - a WebTemplate document type

Compared to a "regular" template, the WebTemplate is based on a Note so that you can directly edit the template from within Nuxeo UI.

The JAXRS API is here to provide REST binding that allow to retrieve resources associated to a template or a template base document.

For example if you template is HTML based and requires the use of a CSS, you may want to attach the CSS to you template using the standard attachement system. And you may also want to override the CSS by attaching a custom version of you CSS directly on the target document.

The JAXRS App provide a simple REST url mapping that handle that, basically if you want to reference a CSS names style.css, you can use the url :

    /nuxeo/site/templates/doc/{docUUID}/resource/{templateName}/style.css

Then the style.css resources will be taken from the document or from the template document associated to it.

From within the template, you can reference this url via the additional context function :

    ${jaxrs.getResourceUrl("style.css")}
