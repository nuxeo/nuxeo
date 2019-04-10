<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Extension point ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span>
  <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}" title="Go to parent component">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a>

<h2>Documentation</h2>
${nxItem.documentationHtml}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>

<h2>Descriptors</h2>
<ul>
  <#list nxItem.descriptors as descriptor>
  <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(descriptor)}"/>
  <li>Javadoc: <a href="${javaDocBaseUrl}/javadoc/${descriptor?replace('.','/')}.html" target="_new">${descriptor}</a>
  </#list>
</ul>

<h2>Contributions</h2>
<#if nxItem.extensions?size gt 0>
  <ul>
    <#list nxItem.extensions as contrib>
    <li>
      <a href="${Root.path}/${distId}/viewContribution/${contrib.id}">${contrib.id}</a>
    </li>
    </#list>
  </ul>
<#else>
No known contributions.
</#if>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
