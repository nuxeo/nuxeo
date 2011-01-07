<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Seam component ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Seam component <span class="componentTitle">${nxItem.name}</span></h1>

<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=true/>

<h2>Scope</h2>
${nxItem.scope}

<h2>Implementation</h2>
<#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(nxItem.className)}"/>
<#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${nxItem.className?replace('.','/')}.html"/>
<p>Javadoc: <a href="${javaDocUrl}" target="_new">${nxItem.className}</a></p>

<#assign hasInterface=false/>
<#list nxItem.interfaceNames as iface>
  <#if iface != nxItem.className>
    <#assign hasInterface=true/>
    <#break>
  </#if>
</#list>

<#if hasInterface>
<h2>Interfaces</h2>
<ul>
  <#list nxItem.interfaceNames as iface>
  <#if iface != nxItem.className>
  <li>
    <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(iface)}"/>
    <#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${iface?replace('.','/')}.html"/>
    Javadoc: <a href="${javaDocUrl}" target="_new">${iface}</a>
  </li>
  </#if>
  </#list>
</ul>
</#if>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
