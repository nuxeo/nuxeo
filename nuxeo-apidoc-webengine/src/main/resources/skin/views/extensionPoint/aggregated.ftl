<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1> View Extension Point <span class="componentTitle">${nxItem.id}</span>
<#if This.nxArtifact.component??>
  <A href="${Root.path}/${distId}/viewComponent/${This.nxArtifact.component.id}/" title="go to parent component"> <img src="${skinPath}/images/up.gif"/> </A>
</#if>
</h1>

<#include "/views/extensionPoint/extensionPointMacros.ftl">

<@viewExtensionPoint extensionPointWO=This expanded=false/>

</@block>

</@extends>