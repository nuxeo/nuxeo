<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
	<id>urn:nuxeo:${ri.name.name}:${xp.name}:contribs</id>
	<title>Contributions to ${xp.name}</title>
<#list contribs as contrib>
	<entry>
		<#assign rcomp = contrib.component/>
		<id>urn:nuxeo:components:${rcomp.name.name}:contribs:${contrib.id}</id>
		<title>${contrib.id}</title>		
		<link rel="edit" href="${Context.URL}/../../../../components/${rcomp.name.name}" />
		<link rel="self" href="${Context.URL}/../../../../components/${rcomp.name.name}" />
		<link rel="xpoints" href="${Context.URL}/../../../../components/${rcomp.name.name}/xpoints" />
		<link rel="contribs" href="${Context.URL}/../../../../components/${rcomp.name.name}/contribs" />		
		<summary type="html">
			<#assign text>
			<b>Owner:</b> <a href="${Context.URL}/../../../../components/${rcomp.name.name}/html">${rcomp.name.name}</a>
			<br>
			<b>Target:</b> <a href="${Context.URL}/../../../../components/${contrib.targetComponent.name}/html">${contrib.targetComponent.name}</a>
			<br>
			<b>Extension Point:</b> ${xp.name}
			${contrib.documentation}
			</#assign>
			${text?html}
		</summary>
	</entry>
</#list>
</feed>
