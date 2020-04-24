<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Bundle <span class="componentTitle">${nxItem.id}</span></h1>

<div class="tabscontent">

  <h2>Components</h2>
  <#if nxItem.components?size == 0>
    No components.
  <#else>
    <ul class="nolist">
      <#list nxItem.components as component>
      <li><a class="tag components" href="${Root.path}/${distId}/viewComponent/${component.name}">${component.name}</a></li>
      </#list>
    </ul>
  </#if>

  <h2>Maven artifact</h2>
  <table class="listTable">
    <tr><th>file</th><td>${nxItem.fileName}</td></tr>
    <tr><th>groupId</th><td>${nxItem.artifactGroupId}</td></tr>
    <tr><th>artifactId</th><td>${nxItem.artifactId}</td></tr>
    <tr><th>version</th><td>${nxItem.artifactVersion}</td></tr>
  </table>

  <h2>Manifest</h2>
  <div>
    <pre><code>${nxItem.manifest}</code></pre>
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
