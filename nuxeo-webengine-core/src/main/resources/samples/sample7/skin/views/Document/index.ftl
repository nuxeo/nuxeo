<#-- we reuse base.ftl from base module -->
<@extends src="base.ftl">

<@block name="content">
    <h2>${Document.title}</h2>
    <div>Document ID: ${Document.id}
    <div>Document path: ${Document.path}
    <div>Document name: ${Document.name}
    <div>Document type: ${Document.type}

    <#-- we redefine the nested block info by adding a link to another view named 'info' on the document -->
    <@block name="info">
    <#-- look how the builtin view service adapter is used to locate the 'info' view -->
    <a href="${This}/@views/info">More Info</a>
    </@block>

  <#if Document.isFolder>
    <hr>
    <div>
    Document children:
    <ul>
    <#list Document.children as doc>
      <li> <a href="${This.path}/${doc.name}">${doc.name}</a>
    </#list>
    </ul>
    </div>
  </#if>

</@block>
</@extends>
