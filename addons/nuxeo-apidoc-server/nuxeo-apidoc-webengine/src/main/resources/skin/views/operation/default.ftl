<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Operation ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Operation <span class="componentTitle">${nxItem.name}</span> (${nxItem.label})</h1>

<h2>Description</h2>
<div class="description">
${nxItem.description}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>
</div>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

<h2>General information</h2>
<table class="listTable">
  <tr><th> Operation id: </th><td> ${nxItem.name?html} </td></tr>
  <tr><th> Category: </th><td> ${nxItem.category?html} </td></tr>
  <tr><th> Label: </th><td> ${nxItem.label?html} </td></tr>
  <tr><th> Requires: </th><td> ${nxItem.requires} </td></tr>
  <tr><th> Since: </th><td> ${nxItem.since} </td></tr>
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
    <td><#if para.isRequired()><b></#if>${para.name}<#if para.isRequired()><b></#if></td>
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
  <tr><th>Inputs: </th><td> ${This.getInputsAsString(nxItem)} </td></tr>
  <tr><th>Outputs: </th><td> ${This.getOutputsAsString(nxItem)} </td></tr>
</table>

<h2>Implementation information</h2>
<table class="listTable">
  <tr><th> Implementation class: </th><td> ${nxItem.operationClass?html} </td></tr>
  <tr><th> Contributing component: </th><td>
   <#if nxItem.contributingComponent=="BuiltIn">
      ${nxItem.contributingComponent?html}
   <#else>
     <A href="${Root.path}/${distId}/viewContribution/${nxItem.contributingComponent}--operations">${nxItem.contributingComponent}</A>
   </#if>
   </td></tr>
</table>

<h2>JSON definition</h2>
<p><a href="${Root.path}/../automation/${nxItem.name}" target="_new">JSON definition</a></p>

</@block>
</@extends>
