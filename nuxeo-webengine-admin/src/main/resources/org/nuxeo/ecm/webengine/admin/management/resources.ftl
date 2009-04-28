<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
	<id>urn:nuxeo:resources</id>
	<title>${root!"Resources"}</title>
<#list resources as rs>
	<entry>
		<id>urn:nuxeo:resources:${rs.name}</id>
		<title>${rs.name}</title>
		<updated>${rs.lastModified()}</updated>		
		<link rel="edit" href="${Context.URL}/${rs.name}" />
		<link rel="self" href="${Context.URL}/${rs.name}" />
		<link rel="alternate" href="${Context.URL}/${rs.name}" />
	</entry>
</#list>
</feed>
