<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
	<id>urn:nuxeo:urn:nuxeo:${ri.name.name}:contribs</id>
	<title>Contributions in ${ri.name.name}</title>
<#list contribs as contrib>
	<entry>
		<id>urn:nuxeo:${ri.name.name}:contribs:${contrib.id}</id>		
		<title>${contrib.id}</title>
		<summary type="html">
			<#assign text>
			<b>Owner:</b> <a href="${Context.URL}/../../../components/${ri.name.name}/html">${ri.name.name}</a>
			<br>
			<b>Target:</b> <a href="${Context.URL}/../../../components/${contrib.targetComponent.name}/html">${contrib.targetComponent.name}</a>
			<br>
			<b>Extension Point:</b> <a href="${Context.URL}/../../../components/${contrib.targetComponent.name}/xpoints">${contrib.extensionPoint}</a>
			<br>
			${contrib.documentation}
			</#assign>
			${text?html}
		</summary>
	</entry>
</#list>
</feed>
