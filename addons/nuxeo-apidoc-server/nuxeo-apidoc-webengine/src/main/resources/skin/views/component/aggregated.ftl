<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Component <span class="componentTitle">${nxItem.id}</span></h1>

<div class="tabscontent">
  <#assign description=docs.getDescription(Context.getCoreSession())/>
  <#include "/views/component/macros.ftl">
  <@viewComponent componentWO=This />
</div>

</@block>

</@extends>