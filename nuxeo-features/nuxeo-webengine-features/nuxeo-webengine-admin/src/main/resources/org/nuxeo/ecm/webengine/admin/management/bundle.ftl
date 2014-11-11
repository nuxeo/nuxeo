<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:nx="http://www.nuxeo.org/server_management">
	<id>urn:nuxeo:bundles:${bundle.symbolicName}</id>
	<title>Bundle ${bundle.symbolicName}</title>
	<title>${bundle.symbolicName}</title>
	<updated>${bundle.lastModified}</updated>		
	<link rel="self" href="${Context.URL}" />
	<link rel="file" href="${Context.URL}/file" />
	<link rel="file" href="${Context.URL}/manifest" />
	<nx:state>${bundle.state}</nx:state>
	<#list components a comp>
	<entry>
	<entry>
		<id>urn:nuxeo:components:${comp.name.name}</id>
		<title>${comp.name.name}</title>
		<link rel="edit" href="${Context.URL}/${comp.name.name}" />
		<link rel="self" href="${Context.URL}/${comp.name.name}" />
		<link rel="alternate" href="${Context.URL}/${comp.name.name}" />
		<link rel="xpoints" href="${Context.URL}/${comp.name.name}/xpoints" />
		<link rel="contribs" href="${Context.URL}/${comp.name.name}/contribs" />		
		<summary>
			${comp.documentation}
		</summary>
	</entry>
	</entry>
	</#list>
</feed>
