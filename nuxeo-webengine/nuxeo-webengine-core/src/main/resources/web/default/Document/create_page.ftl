<@extends src="base.ftl">
<@block name="header"><h1><a href="${basePath}">Create Document</a></h1></@block>
<@block name="content">

<#if Document.facets?seq_contains("Folderish")>
  <div id="create_form">
  <form action="${This.urlPath}/@@create">
    Create a new child named <input type="text" name="name" value="">
    of type
<select name="doctype">
  <#list API.getSortedDocumentTypes() as type>
    <option value="${type.name}">${type.name}</option>
  </#list>
</select>
    <input type="submit" value="Create">
  </form>
  </div>
<#else>
Cannot create documents under non container documents
</#if>


</@block>
</@extends>
