<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:nx="http://www.nuxeo.org/server_management">
	<id>urn:nuxeo:components</id>
	<title>Components</title>
<#list components as comp>
	<entry>
		<id>urn:nuxeo:components:${comp.name.name}</id>
		<title>${comp.name.name}</title>
		<link rel="edit" href="${Context.URL}/${comp.name.name}" />
		<link rel="self" href="${Context.URL}/${comp.name.name}" />
		<link rel="alternate" href="${Context.URL}/${comp.name.name}/html" />
		<link rel="xpoints" href="${Context.URL}/${comp.name.name}/xpoints" />
		<link rel="contribs" href="${Context.URL}/${comp.name.name}/contribs" />
		<nx:state>${comp.state}</nx:state>
		<nx:persisted>${comp.persistent?string}</nx:persisted>		
		<summary type="html">
			${comp.documentation}
		</summary>
	</entry>
</#list>
</feed>
