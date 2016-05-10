<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Operation ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Operation <span class="componentTitle">${nxItem.name}</span> (${nxItem.label})</h1>

<div class="tabscontent">

  <@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

  <table class="listTable">
    <tr><th>Operation id</th><td> ${nxItem.name?html} </td></tr>
    <#if nxItem.aliases> <tr><th>Aliases</th><td>[<#list nxItem.aliases as alias> ${alias} </#list>]</td></tr></#if>
    <tr><th>Category</th><td> ${nxItem.category?html} </td></tr>
    <tr><th>Label</th><td> ${nxItem.label?html} </td></tr>
    <tr><th>Requires</th><td> ${nxItem.requires} </td></tr>
    <tr><th>Since</th><td> ${nxItem.since} </td></tr>
  </table>

  <h2>Parameters</h2>
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

  <h2>Signature</h2>
  <table class="listTable">
    <tr><th>Inputs</th><td> ${This.getInputsAsString(nxItem)} </td></tr>
    <tr><th>Outputs</th><td> ${This.getOutputsAsString(nxItem)} </td></tr>
  </table>

  <h2>Implementation information</h2>
  <table class="listTable">
    <tr><th>Implementation class</th><td> ${nxItem.operationClass?html} </td></tr>
    <tr><th>Contributing component</th><td>
     <#if nxItem.contributingComponent=="BuiltIn">
        ${nxItem.contributingComponent?html}
     <#else>
       <a href="${Root.path}/${distId}/viewContribution/${nxItem.contributingComponent}--operations">${nxItem.contributingComponent}</a>
     </#if>
     </td></tr>
  </table>

  <h2>JSON definition</h2>
  <p><a href="${Root.path}/../automation/${nxItem.name}" class="button" target="_new">Generate JSON definition</a></p>

  <div class="description">
  <h2>Description</h2>
  ${nxItem.description}
  <@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>
  </div>

</div>
</@block>
</@extends>
