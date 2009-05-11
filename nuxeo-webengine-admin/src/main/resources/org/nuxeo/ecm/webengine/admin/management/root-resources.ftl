<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
	<id>urn:nuxeo:resources</id>
	<title>${root!"Resources"}</title>
	<link rel="schemas" href="${Context.URL}/@schemas" />
	<link rel="components" href="${Context.URL}/@components" />
<#list resources as rs>
	<entry>
		<id>urn:nuxeo:resources:${rs.name}</id>
		<title>${rs.name}</title>
		<updated>${This.getLastModified(rs)}</updated>		
		<link rel="edit" href="${Context.URL}/${rs.name}" />
		<link rel="self" href="${Context.URL}/${rs.name}" />
		<link rel="alternate" href="${Context.URL}/${rs.name}" />
	</entry>
</#list>
</feed>
