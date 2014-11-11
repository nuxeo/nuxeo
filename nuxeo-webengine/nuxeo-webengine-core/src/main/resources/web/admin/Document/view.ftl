<@extends src="base.ftl">
<@block name="content">

<#assign docPath = Document.path />

<p>
<b>Name:</b> ${Document.name}
<br/>
<b>Type:</b> ${Document.type}
<br/>
<b>Id:</b> ${Document.id}
</p>
<p>
${Document.description}
</p>
<hr/>
<A href="${This.urlPath}@@edit">Edit</A> | <A href="${This.urlPath}@@delete">Remove</A>
<hr/>

<#if Document.facets?seq_contains("Folderish")>
  <ul>
  <#list Document.children as child>
    <li><a href="${This.urlPath}/${child.name}">${child.name}</a> - ${child.title}</li>
  </#list>
  </ul>
  <hr/>
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
</#if>

<hr />

<#assign perms = script("Document/listpermissions.groovy") />
<h4>Access Rights</h4>
<#if perms>
<table>
    <thead>
        <tr>
            <th>User or group</th>
            <th>Granted permissions</th>
            <th>Denied permissions</th>
        </tr>
    </thead>
    <#list perms as perm>
    <tr>
        <td>${perm.name}</td>
        <#if perm.granted>
            <td>${perm.permission}</td>
            <td></td>
        <#else>
            <td></td>
            <td>${perm.permission}</td>
        </#if>
    </tr>
    </#list>
</table>
</#if>
<form method="POST" action="${This.urlPath}/@@addpermission">
    Add a permission:
    <select name="action" size="1">
        <option value="grant">Grant</option>
        <option value="deny">Deny</option>
    </select>&nbsp;
    <select name="permission" size="1">
        <option value="Read">Read</option>
        <option value="Write">Write</option>
        <option value="Everything">Everything</option>
    </select>
    to user or group <input type="text" name="user" value="">
    <input type="submit" value="Add permission">
</form>


<#--h4>Root Mappings - <a href="${This.urlPath}/publisher/reload.groovy">Reload Mappings</a></h4>
<table cellspacing="0" cellpadding="0">
<tr><td bgcolor="gray">
<table cellspacing="1" cellpadding="4">
  <tr bgcolor="#efefef">
    <td>Aplication Id</td>
    <td>Web Path</td>
    <td>Actions</td>
  </tr>
  <#list Engine.documentMapper.getMappingsForDocument(docPath) as mapping >
  <tr bgcolor="white">
    <td>${mapping[0]}</td>
    <td>${mapping[1]}</td>
    <td><a href="${This.urlPath}/publisher/remove.groovy?docpath=${docPath}&webapp=${mapping[0]}&path=${mapping[1]}">Remove</a></td>
  </tr>
  </#list>
  <tr bgcolor="white">
      <form action="${This.urlPath}/publisher/add.groovy">
    <td colspan="3" align="right">
      Application Id: <input type="text" name="webapp" value="*"/>
      Root Name: <input type="text" name="path" value="${This.name}"/>
      <input type="hidden" name="docpath" value="${docPath}" />
      <input type="submit" value="Create Mapping"/>
    </td>
    </form>
  </tr>
</table>
</td></tr>
</table-->



</@block>
</@extends>
