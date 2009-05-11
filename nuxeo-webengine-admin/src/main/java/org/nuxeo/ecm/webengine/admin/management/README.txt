This module provides a REST API for runtime managament.
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

Here are all the paths exposed:

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

