<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Component ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#include "/views/component/macros.ftl">

<h1>Component <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in bundles">In bundle <a href="${Root.path}/${distId}/viewBundle/${nxItem.bundle.id}">${nxItem.bundle.id}</a></div>

<div class="tabscontent">

  <#if !nxItem.xmlPureComponent>
  <h2>Implementation</h2>
    <#assign componentClass=nxItem.componentClass/>
    <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(componentClass)}"/>
    <p>
      Javadoc: <a href="${javaDocBaseUrl}/javadoc/${componentClass?replace('.','/')}.html" target="_new">${componentClass}</a>
    </p>
  </#if>

  <h2>Services</h2>
  <ul class="nolist">
    <#list nxItem.serviceNames as service>
    <li><a class="tag services" href="${Root.path}/${distId}/viewService/${service}">${service}</a></li>
    </#list>
  </ul>

  <h2>Extension points</h2>
  <ul class="nolist">
    <#list nxItem.extensionPoints as ep>
    <li><a class="tag extensions" href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.name}</a></li>
    </#list>
  </ul>

  <h2>Contributions</h2>
  <ul class="nolist">
    <#list nxItem.extensions as ex>
    <li><a class="tag contributions" href="${Root.path}/${distId}/viewContribution/${ex.id?url}">${ex.id}</a></li>
    </#list>
  </ul>


  <h2>XML source</h2>
  <div>
    <pre><code>${nxItem.xmlFileContent?html}</code></pre>
  </div>

  <h2>Documentation</h2>
  ${nxItem.documentationHtml}
  <@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>
  <#if Root.canAddDocumentation()>
    <div class="tabsbutton">
      <a class="button" href="${This.path}/doc">Manage Documentation</a>
    </div>
  </#if>

  <@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</div>

</@block>
</@extends>
