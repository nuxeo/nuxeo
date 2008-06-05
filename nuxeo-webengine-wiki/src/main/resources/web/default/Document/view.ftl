<@extends src="base.ftl">
<@block name="content">
<h2>${Document.name}</h2>
<h3>${Document.title}</h3>
<p>
<b>Type:</b> ${Document.type}
<br/>
<b>Id:</b> ${Document.id}
</p>
<p>
${Document.description}
</p>
<hr/>
<A href="${This.urlPath}@@edit">Edit</A><BR/>
<hr/>

<#if Document.facets?seq_contains("Folderish")>
  <ul>
  <#list Document.children as child>
    <li><a href="${This.urlPath}/${child.name}">${child.name}</a> - ${child.title}</li>
  </#list>
  </ul>
  <hr/>
  <form>
  
  </form>
</#if>



</@block>
</@extends>