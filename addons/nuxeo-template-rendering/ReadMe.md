
# Nuxeo Template Rendering

## About Nuxeo Template Rendering
 The Nuxeo Template Rendering is a set of plugins that provides a way to associate a Nuxeo Document with a Template. The Templates are used to render the associated document. Depending on the Template type, a different Template Processor will be used and the resulting rendering can be :

   * an HTML document
   * an XML document
   * an OpenOffice document
   * an MS Office document


Each template processor has his own logic for rendering a Document from a Template :

   * raw processing (FreeMarker or XSLT)
   * merge fields replacement (MS Office / OpenOffice)

This project is an on-going project, supported by Nuxeo.

## Sub-modules organization
The project is splitted in several sub modules :

**nuxeo-template-rendering-api**

API module containing all interfaces.

**nuxeo-template-rendering-core**

Component, extension points and service implementation. This modules only contains template processors for FreeMarker and XSLT.

**nuxeo-template-rendering-jsf**

Contribute UI level extensions: Layouts, Widgets, Views, Url bindings ...

**nuxeo-template-rendering-xdocreport**

Contribute the OpenOffice / DocX processor based on XDocReport. This is by far the most powerfull processor.
See: http://code.google.com/p/xdocreport/

**nuxeo-template-rendering-jxls**

Contribute a template processor for XLS files based on JXLS project. See: http://jxls.sourceforge.net/

**nuxeo-template-rendering-jod**

Contribute JOD Report based template processor for ODT files. This renderer is historical and replaced by xdocreport that is more powerful.

**nuxeo-template-rendering-jaxrs**

Contribute a JAXRS simple API as well as a new WebTemplate doc type that is based on a Note rather than a file.

**nuxeo-template-rendering-sandbox**

Misc code and extensions that are currently experimental.

**nuxeo-template-rendering-package**

Builder for marketplace package. 

## Building

### How to build Nuxeo Template Rendering 
Build the Nuxeo Template Rendering add-on with Maven:

```mvn clean install```

## Deploying 
Nuxeo Template Rendering is available as a package add-on [from the Nuxeo Marketplace] (https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-template-rendering)

## Resources 
### Documentation 
The documentation for Nuxeo Template Rendering is available in our Documentation Center: http://doc.nuxeo.com/x/9YSo 

### Following Project QA Status
Follow the project build status on: http://qa.nuxeo.org/jenkins/job/addons_nuxeo-template-rendering-master/

### Reporting Issues 
You can follow the developments in the Nuxeo Platform project of our JIRA bug tracker, which includes a Template Rendering component: https://jira.nuxeo.com/browse/NXP/component/11405

You can report issues on: http://answers.nuxeo.com/

## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
