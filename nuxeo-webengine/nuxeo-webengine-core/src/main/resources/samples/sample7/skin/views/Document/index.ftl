<@extends src="base.ftl">

<@block name="content">
  <h2>${Document.title}</h2>

<table width="100%" border="1">
  <tr>
    <td>
    <div>Document ID: ${Document.id}
    <div>Document path: ${Document.path}
    <div>Document name: ${Document.name}
    <div>Document type: ${Document.type}
    <hr>
    <#if Document.isFolder>
    <div>
    Document children:
    <ul>
    <#list Document.children as doc>
      <li> <a href="${This.path}/${doc.name}">${doc.name}</a> </li>
    </#list>
    </ul>
    </div>
    </#if>
    </td>
    <td>
      <#-- display here the links available in the current context in category INFO -->
      <ul>
      <b>Tools</b>
      <#list This.getLinks("INFO") as link>
        <li> <a href="${link.getCode(This)}">${link.id}</a> </li>
      </#list>
      </ul>
      <#-- display here the links available in the current context in category TOOLS -->
      <ul>
      <b>Adminitsration</b>
      <#list This.getLinks("TOOLS") as link>
        <li> <a href="${link.getCode(This)}">${link.id}</a> </li>
      </#list>
      </ul>
    </td>
  </tr>
</table>

</@block>
</@extends>
