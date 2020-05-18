<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Operation ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Operation <span class="componentTitle">${nxItem.name}</span> (${nxItem.label})</h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.contributingComponent}">${nxItem.contributingComponent}</a></div>

<div class="tabscontent">
  <@toc />

  <#if nxItem.description?has_content>
    <h2>Description</h2>
    <div class="description">
      ${nxItem.description}
    </div>
  </#if>

  <div class="info">
  <table class="listTable">
    <tr><th>Operation id</th><td> ${nxItem.name?html} </td></tr>
    <#if nxItem.aliases> <tr><th>Aliases</th><td><#list nxItem.aliases as alias><code>${alias}<code></#list></td></tr></#if>
    <tr><th>Category</th><td> ${nxItem.category?html} </td></tr>
    <tr><th>Label</th><td> ${nxItem.label?html} </td></tr>
    <tr><th>Requires</th><td> ${nxItem.requires} </td></tr>
    <tr><th>Since</th><td> ${nxItem.since} </td></tr>
  </table>
  </div>

  <h2>Parameters</h2>
  <div class="parameters">
  <#if nxItem.params?size gt 0>
  <table class="topheaderTable">
    <tr>
      <th>Name</th>
      <th>Description</th>
      <th>Type</th>
      <th>Required</th>
      <th>Default value</th>
    </tr>
  <#list nxItem.params as para>
    <tr>
      <td><#if para.isRequired()><b></#if>${para.name}<#if para.isRequired()></b></#if></td>
      <td>${para.description}</td>
      <td>${para.type}</td>
      <td>${para.isRequired()?string("yes","no")}</td>
      <td>${This.getParamDefaultValue(para)}&nbsp;</td>
    </tr>
  </#list>
  </table>
  <#else>
  <p>No parameters.</p>
  </#if>
  </div>

  <h2>Signature</h2>
  <div class="signature">
  <table class="listTable">
    <tr><th>Inputs</th><td> ${This.getInputsAsString(nxItem)} </td></tr>
    <tr><th>Outputs</th><td> ${This.getOutputsAsString(nxItem)} </td></tr>
  </table>
  </div>

  <h2>Implementation information</h2>
  <div class="implementation">
  <table class="listTable">
    <tr><th>Implementation class</th><td> ${nxItem.operationClass?html} </td></tr>
    <tr><th>Contributing component</th><td>
     <#if nxItem.contributingComponent=="BuiltIn">
        <span class="components">${nxItem.contributingComponent?html}</span>
     <#else>
       <a class="components" href="${Root.path}/${distId}/viewContribution/${nxItem.contributingComponent}--operations">${nxItem.contributingComponent}</a>
     </#if>
     </td></tr>
  </table>
  </div>

<#if json??>
  <h2>JSON definition</h2>
  <div class="json">
    <pre>${json?html}</pre>
  </div>
</#if>

  <@tocTrigger />

</div>
</@block>
</@extends>
