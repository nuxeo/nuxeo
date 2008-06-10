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
    of type <input type="text" name="doctype" value="">
    <input type="submit" value="Create">
  </form>
</#if>


<hr />


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
