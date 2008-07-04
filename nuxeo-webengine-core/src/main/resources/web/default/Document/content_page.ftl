
<#if This.isRoot()>
<h2>${Document.title}</h2>
<#else>
<h2><a href="${This.prev().urlPath}"><img src="/nuxeo/site/files/resources/image/up_nav.gif" alt="Up" border="0"/></a> ${Document.title}</h2>
</#if>

<blockquote>${Document.description}</blockquote>

<#if Document.facets?seq_contains("Folderish")>
  <div id="tree">
  <ul class="treeview">
  <#list Document.children as child>
    <li><a href="${This.urlPath}/${child.name}">${child.name}</a> - ${child.title}</li>
  </#list>
  </ul>
  </div>
</#if>

