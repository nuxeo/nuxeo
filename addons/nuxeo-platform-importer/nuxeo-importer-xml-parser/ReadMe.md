Nuxeo Scan Importer
===================

Document importer using XML as source and  Xpath  + MVEL to configure mappings 

## Introduction

This service let you define a XML parsing logic to create documents according information contained.

General approach of this service is to parse from the start to the end the XML File you give. Here is the parser computation execution logic, for each element parsed into the XML:

* service get the first *document creation activator* matching the element (see description bellow) and executed the document creation as configured, without metadata set. The created document is pushed into a stack. If no *document creation activator* match this element, then no document is created and the stack is not changed. Then move to the next **step**.
* on the same element, for each *metadata injection activator* (see description bellow) matching the element, service execute the *metadata injection activator* logic on the last element into the stack. Then move to the next **step**.
* if a *document creation activator* has been activated on current element, the postCreationAutomationChain configured is executed. Then move to the next **item** in the XML file and go to the first step.

So the service exposes 2 extensions points: 

* document creation activators
* metadata injection activators



## Extension points 

As explained into the description chapter, the configuration of the parsing is based on 2 extension points: *document creation activator* and the *metadata injection activator*. This section gives you an overview of these 2 extension points.

The component exposing these extension points is:

	org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterComponent



### Document creation activator (documentMapping)

Here is a typical contribution of this documentMapping extension point:
```xml
	<extension target="org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterComponent"
	    point="documentMapping">
	    <docConfig tagName="seance">
	      <docType>Workspace</docType>
	      <name>@IdSeance</name>
	      <parent>..</parent>
	      <postCreationAutomationChain>myChain</postCreationAutomationChain>
	    </docConfig>
	</extension>
```
  
You can see that :

* You have to define the xpath selector activator (`tagName`)
* You have to define the document type associated to the activator (`docType`)
* You have to define the target path calculation (`parent`)
* You can define the name calculation based on the context (`name`)
* And you can define the Automation Chain Post Creation execution (`postCreationAutomationChain`)


**XPath Selector Activator (tagName)**

First you must express on which context the activator must be activated. The activation logic is base on XPath Selector syntax given into the `tagName` attribute. The service checks all  XPath Selector of the activator list. For **the first document creation activator matching** the current XML item, his creation logic is executed.

For more information about XPath syntax you can look in the end of this documentation or [here](http://www.xpathtester.com)

The next configuration points (docType, name, parent) are used to create the document.

**Document Type (docType)**

Then you must express the document type that will be create each time your configuration is activated. Here you just give the ID of the Document Type (Folder, File, etc…)

**Parent path (parent)**

Here you must return the path of the parent. The path can be:

* `relative` to the previous document created. For instance `..` set, it will create into the parent of the previous document created - meaning the last document into the stack
* `absolute`. For instance `/default-domain/workspaces/my-workspace` will create the document into the document with this path.
* or any result of an `MVEL expression`. See the section about MVEL expression for more detail. Result accepted are String evaluated as target path (relative or abolute), DocumentModel and maybe DocRef (needed to be tested). MVEL expression is introduced by `${…}`.

**Document Name (name)**

In this parameter, you must give the name of the document to create. If no name is given, then the name set will be the document type name with minus and a number (ex: MyDocType-1). You can set:

* a static string into the name item that will be used as name
* or a result of a complex MVEL expression. See the section about MVEL expression for more detail. MVEL expression is introduced by `${…}`.


### Metadata injection activator

Here is a typical contribution of this documentMapping extension point:
```xml
	  <extension target="org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterComponent"
	    point="attributeMapping">
	    <attributeConfig tagName="titre" docProperty="dc:title" xmlPath="text()"/>
	    
	    <attributeConfig tagName="document" docProperty="file:content">
	      <mapping documentProperty="filename">@nom</mapping>
	      <mapping documentProperty="mimetype">mimetype/text()</mapping>
	      <mapping documentProperty="content">@nom</mapping>
	    </attributeConfig>

	  </extension>
```

As explained in the introduction of this documentation, metadata injection activator is:

* a configuration that let you define an activator - based on an XPath Selector expression - on item into the XML parsed. 
* associated to this activator you define:
	* the metadata you fill into the document on the top of the stack,
	* and the computation to fill it.

Here is details about these configuration items:

**XPath Selector Activator (tagName)**

First, you must express on which context the activator must be activated. The activation logic is based on XPath selector syntax given into the `tagName` attribute. Service parse the XML File and for each item, the service check if the xpath selector expression of the activator match the current item. **For each** matched activator the target metadata is filled into the top document in the stack.

For more information about XPath syntax you can look in the end of this documentation or [here](http://www.xpathtester.com)

The next configuration points (`docProperty`, `xmlPath`, `mapping`) are used to express which and how to fill the metadata.

**Target field to fill (docProperty)**

In this attribute you set the metadata you want to fill. As everywhere in Nuxeo the metadata description is based on the schema prefix where is defined the metadata and the xpath of metadata into the schema (ex: dc:title)

**Computation to fill the metadata (xmlPath)**

Here you defined how to fill the metadata when the activator match an element.

The computation can be a:

* text() selector fill the content of an XML item or the attribute selected into the `tagName`
* or a relative XPath selector based on the XML item that has activated this configuration (ex: ../@contentId)
* or a result of a complex MVEL expression. See the section about MVEL expression for more detail.

  
<!--BREAK-->


## XPath Selector basics

XPath, the XML Path Language, is a query language for selecting nodes into an XML document (src: Wikipedia). We used this syntax for the activation logic. 

Here is some basics:

XPath selector | XML selection (bold element) | Comment
:-------------:|------------------------------|:---------:
test2 | …<br>\<test1><div style="margin-left: 10px;">**\<test2>**<div style="margin-left: 10px;">\<test3><div style="margin-left: 10px;">…</div></div></div> | Selecting an XML item,<br> use the name of the item
@test| …<br>\<test1><div style="margin-left: 10px;">\<test2 **test**='toto'><div style="margin-left: 10px;">\<test3><div style="margin-left: 10px;">...</div></div></div> | Selecting an attribute, <br>use the name of the attribute<br> preceded by @
test1/test2| …<br>\<test1><div style="margin-left: 10px;">**\<test2>**<div style="margin-left: 10px;">\<test2><div style="margin-left: 10px;">…</div></div></div> | Selecting an XML item that as<br> a specified XML item as<br> parent<br> (second test2 item is not<br> selected as parent is test2)
test2[test] | …<br>\<test1><div style="margin-left: 10px;">**\<test2 test='value'>**<div style="margin-left: 10px;">\<test2 tutu='titi'><div style="margin-left: 10px;">…</div></div></div> | Selecting an XML item <br> with a given attribute<br>(second test2 item is not <br>selected as has not test<br> attribute)
test2[test='value1'] | …<br>\<test1><div style="margin-left: 10px;">**\<test2 test='value1'>**<div style="margin-left: 10px;">\<test2 test='value2'><div style="margin-left: 10px;">…</div></div></div> | Selecting an XML item with a given<br> attribute and a given value for<br> this attribute (second test2 item is not <br>selected as has value2 as<br> value for test)
test2/@test | …<br>\<test1><div style="margin-left: 10px;">\<test2 **test='value1'**><div style="margin-left: 10px;">\<test3><div style="margin-left: 10px;">…</div></div></div> | Selecting an attribute contained<br> into a given item<br>


## MVEL expression Basics

MVEL expression let you define complex computation based on the java syntax.

For instance, you can create a string as usual:
```java
	String test = "This is a String";
```

You can also call standard static method for instance (semicolon is optional)
```java
	System.out.println("Hello world!")
```

You have also access to some elements into your code that lets you interact with the parser and the context of the evaluation. Here is the list of objects available into the context:

 - `currentDocument` : last created DocumentModel
 - `changeableDocument` : document being created
 - `currentElement` : last parsed DOM4J tag Element
 - `xml` : XML input document as parsed by DOM4J
 - `map` : Mapping between DOM4J ELements and associated created DocumentModel (Element object is the key)
 - `root` : root DocumentModel where the import was started
 - `docs` : list of imported DocumentModels
 - `session` : CoreSession
 - `source` : source file (java.io.File object) or source input stream (java.io.InputStream object) being parsed.
 - `Fn` : utility functions


Here is some interesting functions available in Fn:

Function                                           | Comment 
:--------------------------------------------------|:--------
`Fn.mkdir(rootPath, separator, value, documentType)` | This function is equivalent to mkdir -p in shell syntax. `rootPath` + `value` split by the given `seperator` is used to express the document structure to create. `documentType` is the document type that will be created for missing structure. For instance, let's have a repository containing `/default-domain` document:<br> calling <code>Fn.mkdir('/default-domain/1', '&#124;@', '2&#124;@3&#124;@4', 'Folder')</code> will create folders `/default-domain/1`, `/default-domain/1/2`, `/default-domain/1/2/3`
`Fn.parseDate(source, format)` | This function will parse the `source` date string with the given `format` compatible with the `java.text.SimpleDateFormat` class.


## Advanced examples

### Document Creation activators

#### Example 1

This following configuration:
```xml
    <extension target="org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterComponent"
      point="documentMapping">

      <docConfig tagName="html">
        <docType>Instruction</docType>
        <parent><![CDATA[ #{
          nodes = xml.selectNodes('//meta[@name=\'RCDirection\']/@content');
          String parent = nodes.get(0).getText();
          return Fn.mkdir(root, '/', parent ,'StructureFolder');
        }]]></parent>
        <name><![CDATA[ #{
          String valueFound = xml.selectNodes("//meta[@name='RCIdentifiant']/@content")[0].getText();
          String name = valueFound.replace(' ', '').replace('/', '-');
          return name;
        }]]></name>
        <postCreationAutomationChain>testBJA</postCreationAutomationChain>
      </docConfig>

    </extension>
```

with the following xml fragment:
```xml
	<html>
	  <head>
	    <meta name="RCDirection" content="Dir1/Sec1.1" />
	    <meta name="RCIdentifiant" content="DGAL/C98 - 8010" />
	  </head>
	...
```

Will be equivalent to this following code ():
```java
    // This computation is because the parent evaluation with the Fn.mkdir part.
    // This happends if the Dir1/Sec1.1 documents doesn't exist in root 
    // => see Fn.mkdir description above
    String path = root.pathAsString;
	DocumentModel doc = session.createDocumentModel(path, "Dir1", "StructureFolder");
	doc = session.createDocument(doc);
	
	path = doc.getPathAsString();
	doc = session.createDocumentModel(path, "Sec1.1", "StructureFolder");
	doc = session.createDocument(doc);
	
	// Here is because the document creation activation
	// Parent MVEL expression return the result of the mkdir
	path = doc.getPathAsString();
	// node selected into the xml is "DGAL/C98 - 8010"
	// MVEL expression remove space and replace slash by minus
	String name = "DGA-C98-8010";
	doc = session.createDocumentModel(path, name, "Instruction");
```

#### Example 2

This following configuration:
```xml
	  <extension target="org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterComponent"
	      point="documentMapping">
	    <docConfig tagName="seance">
	      <docType>WebDelibSeance</docType>
	      <name>@IdSeance</name>
	      <parent><![CDATA[ #{
	        idDepot = xml.selectNodes('//@idDepot')[0].getText();
	        query = 'SELECT * FROM WebDelibActe WHERE webdelib_common:adu_id = \'' + idDepot + '\'';
	        org.nuxeo.ecm.core.api.DocumentModelList previousVersionActes = session.query(query);
	        org.nuxeo.ecm.core.api.DocumentModel previousVersionActe = null;
	        for (previousVersionActe : previousVersionActes) {
	          session.removeDocument(previousVersionActe.getRef());
	        }
	        	
	        String dateStr = currentElement.selectNodes('dateSeance/text()')[0].getText().substring(0,10);
	        return Fn.mkdir(root,'-',dateStr,'WebDelibStructure');
	      }]]>
	      </parent>
	    </docConfig>
	  </extension>
```
with the following xml fragment:
```xml
	<?xml version="1.0" encoding="UTF-8"?>
	<depot idDepot="3" xmlns:webdelibdossier="http://www.adullact.org/webdelib/infodossier/1.0" xmlns:xm="http://www.w3.org/2005/05/xmlmine" date="10/11/2012">
	  <seance idSeance="12"> <!--identifiant de la seance-->
	    <typeSeance>Conseil Général</typeSeance> <!--type de la séance-->
	    <dateSeance>2013-02-07 14:00:00</dateSeance> <!--date de la séance-->
```


Will first cleanup each document with id 3 (idDepot attribute value) because these lines in configuration:
```java
	        idDepot = xml.selectNodes('//@idDepot')[0].getText();
	        query = 'SELECT * FROM WebDelibActe WHERE webdelib_common:adu_id = \'' + idDepot + '\'';
	        org.nuxeo.ecm.core.api.DocumentModelList previousVersionActes = session.query(query);
	        org.nuxeo.ecm.core.api.DocumentModel previousVersionActe = null;
	        for (previousVersionActe : previousVersionActes) {
	          session.removeDocument(previousVersionActe.getRef());
	        }
```        
and then create a WebDelibSeance with a name 12 and into the `root/2013/02/07` container (if missing container WebDelibStructure is created)

### Metadata injection activators
```xml
    <attributeConfig tagName="titre" docProperty="dc:title"        
      xmlPath="text()" />
```      
____
```xml   
    <attributeConfig tagName="seance" docProperty="webdelib_common:adu_id"
      xmlPath="../@idDepot" />
```
____
```xml   
    <attributeConfig tagName="dateSeance" docProperty="webdelibseance:date_seance"
      xmlPath="#{
      String date = currentElement.selectNodes('text()')[0].getText().trim();
      if (date.length() == 10) {
        return Fn.parseDate(date, 'yyyy-MM-dd')
      }
      return Fn.parseDate(date, 'yyyy-MM-dd HH:mm:ss')
      }" />
```
____
```xml
    <attributeConfig tagName="//seance/document[@type='convocation']"
      docProperty="file:content">
      <mapping documentProperty="filename">@nom</mapping>
      <mapping documentProperty="mimetype">mimetype/text()</mapping>
      <mapping documentProperty="encoding">encoding/text()</mapping>
      <mapping documentProperty="content">@nom</mapping>
    </attributeConfig>
```
____
```xml
    <attributeConfig tagName="//dossierActe" docProperty="webdelibacte:idActe"
      xmlPath="@idActe" />
```
____
```xml
    <attributeConfig tagName="//meta[@name='RCIdentifiant']" docProperty="dc:title"
      xmlPath="#{
              valueFound = xml.selectNodes('//meta[@name=\'RCIdentifiant\']/@content')[0].getText();
              String title = valueFound.replace(' ', '');
              return title;
               }" />
```
____
```xml
    <attributeConfig tagName="//meta[@name='RCDate']" docProperty="gedeiCommun:date_publication"
      xmlPath="#{
              valueFound = currentElement.attribute('content').getValue().trim();
              return Fn.parseDate(valueFound, 'yyyy-MM-dd');
              }" />
```
____
```xml
    <attributeConfig tagName="body" docProperty="inst:diffusion"
      xmlPath="#{
        return 'members';
              }" />
```
____
```xml
    <attributeConfig tagName="//meta[@name='RCDescription']" docProperty="inst:resume"
      xmlPath="@content" />
```
____
```xml
     <attributeConfig tagName="body"
       docProperty="file:content">
      <mapping documentProperty="filename">#{return source.getName()}</mapping>
      <mapping documentProperty="content">
        #{ return source.getName(); }
      </mapping>
    </attributeConfig>
```

### Full examples

You can look the unit test of the [XML Parser project](https://github.com/nuxeo/nuxeo-platform-importer/tree/master/nuxeo-importer-xml-parser/src/test) and also the [Adullact WebDelib addon](https://github.com/nuxeo/nuxeo-adullact/tree/master/nuxeo-adullact-webdelib-parent/nuxeo-adullact-webdelib-core/src) using deeply this service.


# Calling the service

Finally how you call the parser service ? As usual in Nuxeo, this is quite simple:
```java
    DocumentModel root = session.getRootDocument();
	File xml = File("/where/is/my/umbrella");
    XMLImporterService importer = Framework.getLocalService(XMLImporterService.class);
    importer.importDocuments(root, xml);
```
But you can also give a zip file and the service will parse each xml document inside:
```java
    File zipXml = FileUtils.getResourceFileFromContext("/it/is/in/the/kitchen/export.zip");
    DocumentModel root = session.getRootDocument();

    XMLImporterService importer = Framework.getLocalService(XMLImporterService.class);
    importer.importDocuments(root, zipXml);
```

### Updating existing documents

The importer can update properties for documents already existing in a repository. The criterion for checking that a document exists is by PathRef - meaning that when the importer encounters a document which has the same computed path as an existing document, the importer will update the existing document with the information mapped from the source file. To enable this option add *updateExistingDocuments="true* to a docConfig definition. 
Example:
```xml
	<extension target="org.nuxeo.ecm.platform.importer.xml.parser.XMLImporterComponent" point="documentMapping">
		<docConfig tagName="seance" updateExistingDocuments="true">
		...
```
### Overwrite list attributes while updating existing documents

The default behavior will append any list items found while updating an existing document. To overwrite a list attribute with items from a new source file, add *overwrite="true"* to the attributeConfig definition for the list attribute. Note that it will be necessary to have a separate attributeConfig for a list member. For example: 
```xml
	<attributeConfig tagName="subjects" docProperty="dc:subjects" overwrite="true" />
	<attributeConfig tagName="subject" docProperty="dc:subjects" xmlPath="text()" />
```
### Deferred save

During an import, documents are placed on a stack and saved either when the document is created or updated successfully or at the end of the import process. The default behavior is to save when the document is created or updated. To defer saving objects to the end of an import process, the *importDocuments* method takes a third optional parameter (set to *true*):
```java
	importer.importDocuments(root, xml, true);
```
