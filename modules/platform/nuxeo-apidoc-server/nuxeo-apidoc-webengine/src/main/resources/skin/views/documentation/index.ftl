<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<h1> Documentation</h1>

<div class="tabscontent">

  <span style="float:right">
  <form method="GET" action="${Root.path}/${distId}/doc/filter" >
    <input name="fulltext" value="${searchFilter}" placeholder="What are you looking for ?" type="search">
    <input type="submit" value="filter">
  </form>
  <#if searchFilter??>
    <a href="${Root.path}/${distId}/doc"> [ Reset ] </a>
  </#if>
  </span>

  <#assign categories=docsByCat?keys/>

  <#list categories as category>
   <h3>${category}</h3>
   <ul>
   <#list docsByCat[category] as docItem>
      <li><a href="${Root.path}/${distId}/doc/view/${docItem.getUUID()}">${docItem.title}</a> </li>
   </#list>
   </ul>
  </#list>
</div>

</@block>

</@extends>
