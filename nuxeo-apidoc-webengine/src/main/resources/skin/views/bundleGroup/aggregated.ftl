<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#include "/views/bundle/bundleMacros.ftl">

<H1> View for ${nxItem.artifactType} ${nxItem.id}</H1>
<#assign description=docs.getDescription(Context.getCoreSession())/>
<#assign bundles=This.getBundles()/>

<h2> ${description.title} </h2>

<p><@docContent docItem=description /></p>

${description.title} is composed of ${bundles?size} bundles.

<table>
<#list bundles as bundle>
<tr>
<td>
${bundle.nxArtifact.id}
</td>
<td>
${bundle.associatedDocuments.getDescription(Context.getCoreSession()).title}
</td>
</tr>
</#list>
</table>

<#list bundles as bundle>
<h3> ${bundle.associatedDocuments.getDescription(Context.getCoreSession()).title} </h3>

 <@viewBundle bundleWO=bundle />

</#list>


</@block>

</@extends>