<h2>${Document.title}</h2>
<h3>${Document.type}</h3>

<#if Document.facets?seq_contains("Folderish")>
  <div id="tree">
  <ul class="treeview">
  <#list Document.children as child>
    <li><a href="${This.urlPath}/${child.name}">${child.name}</a> - ${child.title}</li>
  </#list>
  </ul>
  </div>
</#if>

