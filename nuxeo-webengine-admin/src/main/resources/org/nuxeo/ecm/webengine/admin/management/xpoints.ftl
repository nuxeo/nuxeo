<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
	<id>urn:nuxeo:urn:nuxeo:${ri.name.name}:xpoints</id>
	<title>Extension Points for ${ri.name.name}</title>
<#list xpoints as xp>
	<entry>
		<id>urn:nuxeo:${ri.name.name}:xpoints:${xp.name}</id>		
		<title>${xp.name}</title>
		<link rel="edit" href="${Context.URL}/${xp.name}" />
		<link rel="self" href="${Context.URL}/${xp.name}" />
		<link rel="alternate" href="${Context.URL}/${xp.name}" />
		<summary type="html">
		${xp.documentation}
		</summary>
	</entry>
</#list>
</feed>
