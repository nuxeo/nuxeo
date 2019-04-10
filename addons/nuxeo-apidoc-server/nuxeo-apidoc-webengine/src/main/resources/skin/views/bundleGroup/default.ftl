<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle group ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Bundle group <span class="componentTitle">${nxItem.name}</span></h1>

<div class="tabscontent">

  <h2>Documentation</h2>
  <@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>
  <#if Root.canAddDocumentation()>
    <div class="tabsbutton">
      <a class="button" href="${This.path}/doc">Manage Documentation</a>
    </div>
  </#if>

  <#if nxItem.subGroups?size gt 0>
  <h2>Bundle subgroups</h2>
  <ul>
    <#list nxItem.subGroups as subGroup>
    <li>
      <a href="${Root.path}/${distId}/viewBundleGroup/${subGroup.name}">${subGroup.name}</a>
    </li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.bundleIds?size gt 0>
  <h2>Bundles</h2>
  <ul>
    <#list nxItem.bundleIds as bundleId>
    <li>
      <a href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</a>
    </li>
    </#list>
  </ul>
  </#if>

  <@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</div>

</@block>
</@extends>
