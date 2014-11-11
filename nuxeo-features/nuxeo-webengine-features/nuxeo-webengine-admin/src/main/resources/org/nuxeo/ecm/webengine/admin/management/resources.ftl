<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom" xmlns:nx="http://www.nuxeo.org/server_management">
	<id>urn:nuxeo:resources</id>
	<title>${root!"Resources"}</title>
<#list resources as rs>
	<entry>
		<nx:type>${rs.isFile()?string("file","directory")}</nx:type>
		<id>urn:nuxeo:resources:${rs.name}</id>
		<title>${rs.name}</title>
		<updated>${This.getLastModified(rs)}</updated>		
		<link rel="edit" href="${Context.URL}/${rs.name}" />
		<link rel="self" href="${Context.URL}/${rs.name}" />
		<link rel="alternate" href="${Context.URL}/${rs.name}" />
	</entry>
</#list>
</feed>
