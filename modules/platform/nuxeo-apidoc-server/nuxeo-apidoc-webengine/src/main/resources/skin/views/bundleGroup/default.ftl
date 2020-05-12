<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle group ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Bundle group <span class="componentTitle">${nxItem.name}</span></h1>

<div class="tabscontent">

  <#if nxItem.readmes?size gt 0>
    <h2>Documentation</h2>
    <div class="documentation">
      <ul class="block-list">
        <#list nxItem.readmes as readme>
          <li>
            <div class="block-title">
              ${readme.filename}
            </div>
            <div>
              <pre>${readme.getString()}</pre>
            </div>
          </li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if nxItem.subGroups?size gt 0>
  <h2>Bundle subgroups</h2>
  <ul class="subbroups">
    <#list nxItem.subGroups as subGroup>
    <li>
      <a href="${Root.path}/${distId}/viewBundleGroup/${subGroup.name}">${subGroup.name}</a>
    </li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.bundleIds?size gt 0>
  <h2>Bundles</h2>
  <ul class="groupbundles">
    <#list nxItem.bundleIds as bundleId>
    <li>
      <a href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</a>
    </li>
    </#list>
  </ul>
  </#if>

</div>

</@block>
</@extends>
