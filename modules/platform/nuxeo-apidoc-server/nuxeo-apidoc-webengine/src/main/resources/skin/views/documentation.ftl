<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Documentation for ${nxItem.id}</@block>

<@block name="right">
<h1>Documentation for <span class="componentTitle">${nxItem.id}</span></h1>

<div class="tabscontent">

  <#assign docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

  <#if Root.canAddDocumentation()>
  <p><a href="${This.path}/createForm" class="button"> Add new documentation </a></p>
  </#if>

  <#list docsByCat?keys as category>
   <h2>${category}</h2>

   <#list docsByCat[category] as docItem>
    <div class="docContent">
      <h3><a name="${docItem.id}"> </a>${docItem.title}</h3>
      <#if Root.isEditor()>
        <a href="${This.path}/editForm/${docItem.getUUID()}" class="button">Edit</a>
        <div style="float:right">
          <form method="POST" action="${This.path}/deleteDocumentation">
            <input type="hidden" name="uuid" value="${docItem.getUUID()}" />
            <input type="submit" value="Delete" onclick="return confirm('Really delete?')"/>
          </form>
        </div>
      </#if>
      <#include "docItemView.ftl">
    </div>
   </#list>
  </#list>

</div>

</@block>
</@extends>
