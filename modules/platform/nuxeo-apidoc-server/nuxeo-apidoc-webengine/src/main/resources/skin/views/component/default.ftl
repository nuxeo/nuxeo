<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Component ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Component <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in bundles">In bundle <a href="${Root.path}/${distId}/viewBundle/${nxItem.bundle.id}">${nxItem.bundle.id}</a></div>

<div class="tabscontent">

  <#if nxItem.documentationHtml?has_content>
    <h2>Documentation</h2>
    <div class="documentation">
      ${nxItem.documentationHtml}
    </div>
  </#if>

  <#if nxItem.requirements?size gt 0>
    <h2>Requirements</h2>
    <ul class="nolist" id="requirements">
      <#list nxItem.requirements as req>
      <li><a class="tag components" href="${Root.path}/${distId}/viewComponent/${req}">${req}</a></li>
      </#list>
    </ul>
  </#if>

  <#if !nxItem.xmlPureComponent>
  <h2>Implementation</h2>
    <#assign componentClass=nxItem.componentClass/>
    <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(componentClass)}"/>
    <p>
      Javadoc: <a href="${javaDocBaseUrl}/javadoc/${componentClass?replace('.','/')}.html" target="_new">${componentClass}</a>
    </p>
  </#if>

  <#if nxItem.serviceNames?size gt 0>
  <h2>Services</h2>
  <ul class="nolist">
    <#list nxItem.serviceNames as service>
    <li><a class="tag services" href="${Root.path}/${distId}/viewService/${service}">${service}</a></li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.extensionPoints?size gt 0>
  <h2>Extension points</h2>
  <ul class="nolist">
    <#list nxItem.extensionPoints as ep>
    <li><a class="tag extensions" href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.name}</a></li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.extensions?size gt 0>
  <h2>Contributions</h2>
  <ul class="nolist">
    <#list nxItem.extensions as ex>
    <li><a class="tag contributions" href="${Root.path}/${distId}/viewContribution/${ex.id?url}">${ex.id}</a></li>
    </#list>
  </ul>
  </#if>

  <h2>XML source</h2>
  <div>
    <pre><code>${nxItem.xmlFileContent?html}</code></pre>
  </div>

</div>

</@block>
</@extends>
