<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1> View Bundle group ${nxItem.id}</h1>
<div class="tabscontent">

  <#include "/views/bundleGroup/macros.ftl">

  <@viewBundleGroup bundleGroupWO=This />
</div>

</@block>

</@extends>