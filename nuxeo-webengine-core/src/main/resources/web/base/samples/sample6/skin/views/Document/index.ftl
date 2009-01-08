<#-- we reuse base.ftl from base module -->
<@extends src="base.ftl">

<@block name="content">
  <h2>${Document.title}</h2>
  <div>Document ID: ${Document.id}</div>
  <div>Document path: ${Document.path}</div>
  <div>Document name: ${Document.name}</div>
  <div>Document type: ${Document.type}</div>

  <p>
    <#-- we redefine the nested block info by adding a link to another view named 'info' on the document -->
    <@block name="info">
    <#-- look how the builtin view service adapter is used to locate the 'info' view -->
    <a href="${This.path}/@views/info">More Info</a>
    </@block>
  </p>

  <#if Document.isFolder>
    <hr/>
    <div>
    Document children:
    <ul>
    <#list Document.children as doc>
      <li> <a href="${This.path}/${doc.name}">${doc.name}</a> </li>
    </#list>
    </ul>
    </div>
  </#if>

</@block>
</@extends>
