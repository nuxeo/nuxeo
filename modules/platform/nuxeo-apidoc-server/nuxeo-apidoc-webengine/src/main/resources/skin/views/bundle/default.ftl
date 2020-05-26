<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Bundle <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in">In bundle group <a href="${Root.path}/${distId}/viewBundleGroup/${nxItem.bundleGroup.id}">${nxItem.bundleGroup.name}</a></div>

<div class="tabscontent">
  <@toc />

  <#if nxItem.readme?has_content || nxItem.parentReadme?has_content>
    <h2>Documentation</h2>
    <div class="documentation">
      <ul class="block-list">
        <#if nxItem.readme?has_content>
          <li>
            <div class="block-title">
              ${nxItem.readme.filename}
            </div>
            <div>
              <pre>${nxItem.readme.getString()}</pre>
            </div>
          </li>
        </#if>
        <#if nxItem.parentReadme?has_content>
          <li>
            <div class="block-title">
              Parent Documentation: ${nxItem.parentReadme.filename}
            </div>
            <div>
              <pre>${nxItem.parentReadme.getString()}</pre>
            </div>
          </li>
        </#if>
      </ul>
    </div>
  </#if>

  <#if nxItem.requirements?size gt 0>
    <h2>Requirements</h2>
    <ul class="nolist" id="requirements">
      <#list nxItem.requirements as req>
      <li><a class="tag bundles" href="${Root.path}/${distId}/viewBundle/${req}">${req}</a></li>
      </#list>
    </ul>
  </#if>

  <h2>Deployment Order</h2>
  <div id="deploymentOrder">
    ${nxItem.deploymentOrder}
  </div>

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

  <h2>Maven Artifact</h2>
  <table class="listTable">
    <tr><th>file</th><td>${nxItem.fileName}</td></tr>
    <tr><th>groupId</th><td>${nxItem.groupId}</td></tr>
    <tr><th>artifactId</th><td>${nxItem.artifactId}</td></tr>
    <tr><th>version</th><td>${nxItem.artifactVersion}</td></tr>
  </table>

  <#if nxItem.manifest?has_content>
    <h2>Manifest</h2>
    <div>
      <pre><code>${nxItem.manifest}</code></pre>
    </div>
  </#if>

  <@tocTrigger />

</div>

</@block>
</@extends>
