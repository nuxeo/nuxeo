
# What is this plugin for ?

This Nuxeo Platform plugin provides a way to associate a Nuxeo Document with a Template.

The Template is a Word XML or OpenOffice document (stored also in a Nuxeo Document).

The Template is used to render the Document.

For exemple you can define in your template a field that will be replaced by the dc:title of the document.

Each format (WordML / OpenOffice) has his own logic for defining merge fields.

In the case of OpenOffice document, you can actually generate the full document since it uses JODReports 
(that use Freemarker to render an ODT file).

# History

This plugin is based on the DocX prototype done a long time ago here : http://hg.nuxeo.org/sandbox/nuxeo-platform-docx-templates/

# Building

	mvn clean install

# Deploying

You first need to deploy the plugin inside your Nuxeo server.

	cp target/nuxeo-platform-rendering-templates-X.Y.Z.jar into nxserver/bindles

You also need to copy the additional libs (there is no Marketplace package for now)

        cp target/libs/*  nxserver/lib/

# Templating format

The main templating engine is now <A href="http://code.google.com/p/xdocreport/">XDocReport</A> using Freemarker engine.

# TODO 

 See the <A href="https://jira.nuxeo.com/browse/NXP-8201">Jira tickets</A>
	
