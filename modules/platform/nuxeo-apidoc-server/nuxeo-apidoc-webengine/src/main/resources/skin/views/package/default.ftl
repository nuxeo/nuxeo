<@extends src="base.ftl">
<@block name="title">Package ${nxItem.title}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Package <span class="componentTitle">${nxItem.title}</span> (${nxItem.name})</h1>

<div class="tabscontent">
  <@toc />

  <h2>General Information</h2>
  <div class="info">
    <table class="listTable">
      <tr>
        <th>Id</th>
        <td id="packageId">${nxItem.id?html}</td>
      </tr>
      <tr>
        <th>Name</th>
        <td id="packageName">${nxItem.name?html}</td>
      </tr>
      <tr>
        <th>Version</th>
        <td id="packageVersion">${nxItem.version?html}</td>
      </tr>
      <#if marketplaceURL??>
      <tr>
        <th>Marketplace Link</th>
        <td><a id="marketplaceLink" href="${marketplaceURL}" target="_blank">${marketplaceURL}</a></td>
      </tr>
      </#if>
    </table>

  </div>

  <h2>Bundles</h2>
  <div id="bundles">
  <#if nxItem.bundles?size gt 0>
    <ul>
      <#list nxItem.bundles as bundle>
      <li><a class="bundles" href="${Root.path}/${distId}/viewBundle/${bundle}">${bundle}</a></li>
      </#list>
    </ul>
  <#else>
    <div>No bundles.</div>
  </#if>
  </div>

  <#if nxItem.dependencies?size gt 0 || nxItem.optionalDependencies?size gt 0 || nxItem.conflicts?size gt 0>
  <div id="alldependencies">
    <#if nxItem.dependencies?size gt 0>
    <h2>Dependencies</h2>
    <ul id="dependencies">
      <#list nxItem.dependencies as dep>
      <li><a class="packages" href="${Root.path}/${distId}/viewPackage/${dep}">${dep}</a></li>
      </#list>
    <ul>
    </#if>
    <#if nxItem.optionalDependencies?size gt 0>
    <h2>Optional Dependencies</h2>
    <ul id="optionalDependencies">
      <#list nxItem.optionalDependencies as dep>
      <li><a class="packages" href="${Root.path}/${distId}/viewPackage/${dep}">${dep}</a></li>
      </#list>
    <ul>
    </#if>
    <#if nxItem.conflicts?size gt 0>
    <h2>Conflicts</h2>
    <ul id="conflicts">
      <#list nxItem.conflicts as dep>
      <li><a class="packages" href="${Root.path}/${distId}/viewPackage/${dep}">${dep}</a></li>
      </#list>
    <ul>
    </#if>
  </div>
  </#if>

<@tocTrigger />

</div>
</@block>
</@extends>
