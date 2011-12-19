
# What is this plugin for ?

This Nuxeo Platform plugin provides a way to associate a Nuxeo Document with a Template.

The Template is a WordXML or OpenOffice document (stored also in a Nuxeo Document).

The Template is used to render the Document.

For exemple you can define in your template a field that will be replaced by the dc:title of the document.

Each format (WordML / OpenOffice) has his own logic for defining merge fields.

In the case of OpenOffice document, you can actually generate the full document since it used JODReports 
(that use Freemarker to render an ODT file).

# Building

	mvn clean install

# Deploying

You first need to deploy the plugin inside your Nuxeo server.

	cp target/nuxeo-platform-rendering-templates-X.Y.Z.jar into nxserver/bindles

You also need to copy the 2 additional libs (there is no Marketplace package for now)

	nxserver/lib/jodreports-2.4.0.jar ( http://jodreports.sourceforge.net/ )

        nxserver/lib/xom-1.2.7.jar ( http://www.cafeconleche.org/XOM/xom-1.2.7.jar)

# Templating format

For now you have to look in src/test/resources/data to find some sample ODT and docX files using in Unit Testing ...

# TODO 

 - Make a set of nice template samples

 - Add Document Composition (i.e. support rendering a Note via a template that provides cover page and Index)

 - Bind audit inside Rendering context

 - Add UI to add a template to any Document Type in Nuxeo

 - Plug converters to choose the output format independently from the template format

 - Plug other existing rendering systems in Nuxeo (Like the DocumentRendering in Seam / JSF) ?

        
        
	
