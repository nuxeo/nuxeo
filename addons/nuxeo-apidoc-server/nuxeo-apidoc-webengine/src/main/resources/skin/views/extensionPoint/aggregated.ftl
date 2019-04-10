<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span>
  of component ${nxItem.component.name?replace(".*\\.", "", "r")}
</h1>

<div class="tabscontent">
  <#include "/views/extensionPoint/macros.ftl">
  <@viewExtensionPoint extensionPointWO=This expand=false/>
</div>

</@block>

</@extends>