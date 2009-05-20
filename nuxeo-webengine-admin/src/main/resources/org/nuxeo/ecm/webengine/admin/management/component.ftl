<?xml version="1.0"?>
<entry xmlns="http://www.w3.org/2005/Atom" xmlns:nx="http://www.nuxeo.org/server_management">
	<nx:state>${component.state}</nx:state>
	<nx:persisted>${component.persistent?string}</nx:persisted>	
	<title>Component ${component.name.name}</title>
	<id>urn:nuxeo:components:${component.name.name}</id>
	<title>${component.name.name}</title>
	<link rel="edit" href="${Context.URL}" />
	<link rel="self" href="${Context.URL}" />
	<link rel="xpoints" href="${Context.URL}/xpoints" />
	<summary>
	${This.summary}
	</summary>
</entry>
