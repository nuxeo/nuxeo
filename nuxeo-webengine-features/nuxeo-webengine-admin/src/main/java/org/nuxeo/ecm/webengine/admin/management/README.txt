This module provides a REST API for runtime managament. 

This service is available only if the runtime property 
org.nuxeo.runtime.rest.management is defined and equals to "true".
To define this property add this line in your nuxeo.properties file:
org.nuxeo.runtime.rest.management=true

The module defines an ATOMPUB service that expose collections to introspect and manage Nuxeo bundles, components, contributions and files resources deployed on a running server.
Clients can introspect the service document to detect the service collections in order to navigate, to get and post data.
Feeds that expose informations like available extension points or contributions in a component, or available components in a bundle can be retrieved by clients by reading custom atom link tags on the feed entries.  
Apart the REST API there is also a HTML interface to browse runtime components.

The REST API is useful for client tools like Eclipse IDE plugins to browse Nuxeo components, to hot deploy new contributions or other resources.
The HTML interface is useful to browse bundles, components, contributions and extension points deployed on the server.

For now only resources (configuration files, schemas) can be posted using the API. In future it will be possible to deploy new extensions and to reload components or bundles too.

The application is designed as a pure JAX-RS application (without using WebEngine object model) but make use of freemarker templates to build response to clients.
In future this application may be moved outside nuxeo-webengine-admin in a dedicated project.

The ATOMPUB service document is available under the /server path of the HTTP server. Ex: at http://localhost:8080/server
The HTML interface is available at /server/html.

Here are all the paths exposed: (note that the base URL is http://host:port/nuxeo/site/server for a JBoss Nuxeo installation) 

GET http://host:port/server - the ATOMPUB service document
GET http://host:port/server/html - the entry point of the Web interface (not REST)
GET http://host:port/server/bundles - the bundles collection
GET http://host:port/server/components - the components collection
GET http://host:port/server/resources - the resources collection
GET http://host:port/server/resources/@schemas - the document schema files (read only)
GET http://host:port/server/resources/@components - the persisted components contributed by the user (read only)
GET http://host:port/server/resources/@reload - reload the resource class loader.
GET http://host:port/server/bundles/{symbolicName} - the bundle entry
GET http://host:port/server/components/{componentName} - the component entry
GET http://host:port/server/components/{componentName}/xpoints - the xpoints provided by a bundle
GET http://host:port/server/components/{componentName}/xpoints - the contributions provided by a bundle
GET http://host:port/server/components/{componentName}/{xpoint} - the feed of all extensions contributed to the {xpoint} extension point
GET http://host:port/server/system_properties - the system properties
GET http://host:port/server/runtime_properties - the runtime properties
POST http://host:port/server/system_properties - define a new system property
POST http://host:port/server/runtime_properties - define a new runtime property
POST http://host:port/server/resources - post a new resource

POST http://host:port/server/components - upload and deploy a new component
DELETE http://host:port/server/components/{name} - remove a persisted component. 
	Only components persisted explicitly by the user (using POST or being added in the ${nxserver.home}/data/extensions directory by hand) can be removed
PUT http://host:port/server/components/{name} - switch the state of the component (activate/deactivate)
	When activating a component all of its extension points becomes available, 
	and all of its extensions are contributed to the target components. 
	When deactivating it, its extension points are disabled and and any contribution made by the component is removed.  


Not yet implemented:

POST http://host:port/server/bundles - upload and install a new bundle
PUT http://host:port/server/bundles/{symbolicName} - switch the state of the bundle (install/uninstall)
DELETE http://host:port/server/bundles/{symbolicName} - completely remove from he server the given bundle 

HTTP return codes are not yet defined. All method will usually return 200 or 500 if an exception is thrown by the server   

--------------------

Here is an example on deploying a scripting core listener:

1. POST http://localhost:8080/nuxeo/site/server/resources?file=script/listener.groovy

System.out.println("Hello from a Groovy listener. Event name: " 
	+ event.name);

2. POST http://localhost:8080/nuxeo/site/server/components

<?xml version="1.0"?>
<component name="test-listener">

  <extension target="org.nuxeo.ecm.core.event.EventServiceComponent" point="listener">
    <listener script="script/listener.groovy"/>
  </extension>

</component>

Now you should have a listener that will print  "ello from a Groovy listener. Event name: ..." each time an event is fired. 

 If you want to redeploy (i.e. delete then deploy again deploy) the listener you should do this:
 
1. DELETE http://localhost:8080/nuxeo/site/server/components/test-listener
2. POST http://localhost:8080/nuxeo/site/server/resources?file=script/listener.groovy
3. POST http://localhost:8080/nuxeo/site/server/components

The DELETE will remove the listener. 
Then the first POST is uploading a new script to be used by the listener contribution.
Then the second POST is deploying the listener again.

Note that it is important to first upload the data needed by the contribution (e.g. a script, a class or other resource) and then to upload the cotnribution itself - this way you are sure your resource files are available to the new contribution


The deployed components are persisted between server restart. In fact all the component files are copied into the directory ${nxserver}/data/components
and any XML file that is presenthere at startup will be deployed as a component.
This means you can use that directory as an administrator to manually deploy components on the server without packaging them in JARs.
Anyway, there are some naming convention you must using this directory to deploy your components.
The file name without the .xml extension of a component file must be the same as the component name. 

Example:
my-listener.xml must contain a <component name="my-listener" ... > component

When deploying components in Nuxeo, these components must have a host bundle that will be used to resolve resources.
This means, a contribution from a component will use the bundle classloader to load classes or resources. 
When you are putting components directly under the component directory they will be deployed as part of the nuxeo-runtime bundle.
So the classloader visibility is the one exposed by nuxeo-runtime bundle.
If you want to use another bundle as the host bundle of a contribution you must create a sub-directory having as name the bundle 
symbolic name and then put the contribution there. In this way you can deploy components as part of any bundle running in the server.
When contributing components in sub-directories - you must use the relative path of the XML file (without the .xml extension) as the component name.

Example:
org.nuxeo.ecm.core.api/my-listener.xml must contain a <component name="org.nuxeo.ecm.core.api/my-listener" ... > component

Here is a possible layout of the ${nxserver}/data/components directory:


components/
	my-listener.xml
	my-types.xml
	org.nuxeo.ecm.core.api/my-listener.xml
	org.nuxeo.ecm.core.api/my-ladapter.xml
	org.nuxeo.ecm.core.io/my-adapter.xml
	...
	

These naming rules are also used by the REST API to know where to deploy a component (in which bundle).
For example if you POST a component definition as follows:

<?xml version="1.0"?>
<component name="org.nuxeo.ecm.core.api/test-listener">

  <extension target="org.nuxeo.ecm.core.event.EventServiceComponent" point="listener">
    <listener script="script/listener.groovy"/>
  </extension>

</component>
		

your component will be put into components/org.nuxeo.ecm.core.api/test-listener.xml which means it will be deployed in the nuxeo-core-api host bundle.
Of course if you are using a name like "test-listener" the component will be put directly under the root - thus in the nuxeo-runtime host bundle.  

Anyway, in Nuxeo standalone and Nuxeo JBoss distributions a shared class loader is used - so that all installed classes will be available in all bundles.
This means it makes no difference if you deploy your component in one bundle host or another.
The only difference is if you use Bundle.getEntry("some/bundle/resource") in your extension point manager to get an entry from the bundle owning the contribution.
In that case it becomes important in which context you deploy your component.     

In order to be able to deploy components that make reference to external resources (that are not in bundle JARs) a special
directory is used bu the framework: ${nxserver}/data/resources.
The content of this directory is visible by any bundle in the system when using RuntimeContext to locate classes or resources.

This means that any resource file or class in that directory can be retrieved by calling RuntimeContext.getResource("the_resource_path") and respectively RuntimeContext.loadClass("theclassname")
Each bundle has an unique RuntimeContext that is attached to an OSGi BundleContext. This context can be retrieved from any component at activation time.   
  
This directory is for example used when you need to contribute a component that make use of an external script like the script listener we used in  the example above.
In this case you need to POST this script to a valid path inside the resources directory. 
To do this you should make this request:
POST http://localhost:8080/nuxeo/site/server/resources?file=script/listener.groovy

This instructs the REST API to upload your resource in resources/script/listener.groovy.
Then this script can be referred in XML contributions as script/listener.groovy.



  

 
