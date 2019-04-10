<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Component ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#include "/views/component/macros.ftl">

<h1>Component <span class="componentTitle">${nxItem.id}</span>
  <a href="${Root.path}/${distId}/viewBundle/${nxItem.bundle.id}" title="Go to parent bundle">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

In bundle <a href="${Root.path}/${distId}/viewBundle/${nxItem.bundle.id}">${nxItem.bundle.id}</a>

<h2>Documentation</h2>
${nxItem.documentationHtml}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>

<#if !nxItem.xmlPureComponent>
<h2>Implementation</h2>
  <#assign componentClass=nxItem.componentClass/>
  <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(componentClass)}"/>
  <p>
    Javadoc: <a href="${javaDocBaseUrl}/javadoc/${componentClass?replace('.','/')}.html" target="_new">${componentClass}</a>
  </p>
</#if>

<h2>Services</h2>
<ul>
  <#list nxItem.serviceNames as service>
  <li><a href="${Root.path}/${distId}/viewService/${service}">${service}</a></li>
  </#list>
</ul>

<h2>Extension points</h2>
<ul>
  <#list nxItem.extensionPoints as ep>
  <li><a href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.name}</a></li>
  </#list>
</ul>

<h2>Contributions</h2>
<ul>
  <#list nxItem.extensions as ex>
  <li><a href="${Root.path}/${distId}/viewContribution/${ex.id?url}">${ex.id}</a></li>
  </#list>
</ul>


<h2>XML source</h2>
<span class="resourceToggle">View ${nxItem.xmlFileName?replace("^/","","r")}</span>
<div class="hiddenResource">
  <pre><code>${nxItem.xmlFileContent?html}</code></pre>
</div>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
