<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Service ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Service <span class="componentTitle">${nxItem.id}</span>
  <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}" title="Go to parent component">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a>

<h2>Documentation</h2>
${nxItem.documentationHtml}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>

<h2>Implementation</h2>
<#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(nxItem.id)}"/>
<#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${nxItem.id?replace('.','/')}.html"/>
<p>Javadoc: <a href="${javaDocUrl}" target="_new">${nxItem.id}</a></p>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
