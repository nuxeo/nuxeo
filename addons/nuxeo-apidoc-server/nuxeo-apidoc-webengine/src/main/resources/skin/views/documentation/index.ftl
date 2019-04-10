<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<h1> Documentation</h1>

<span style="float:right">
<form method="GET" action="${Root.path}/${distId}/doc/filter" >
  <input type="text" name="fulltext" value="${searchFilter}">
  <input type="submit" value="filter">
</form>
<#if searchFilter??>
  <A href="${Root.path}/${distId}/doc"> [ Reset ] </A>
</#if>
</span>

<#assign categories=docsByCat?keys/>

<#list categories as category>
 <h3>${category}</h3>
 <ul>
 <#list docsByCat[category] as docItem>
    <li><A href="${Root.path}/${distId}/doc/view/${docItem.getUUID()}">${docItem.title}</A> </li>
 </#list>
 </ul>
</#list>

</@block>

</@extends>