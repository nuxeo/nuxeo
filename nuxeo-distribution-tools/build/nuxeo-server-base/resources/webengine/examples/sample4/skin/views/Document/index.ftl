<@extends src="base.ftl">

<@block name="content">
    <h2>${Document.title}</h2>
    <div>Document ID: ${Document.id}
    <div>Document path: ${Document.path}
    <div>Document name: ${Document.name}
    <div>Document type: ${Document.type}

    <#-- Here we declare a nested block. Look in sample6 how nested block can be redeclared -->
    <@block name="info">

    <div>
    Document schemas:
    <ul>
    <#list Document.schemas as schema>
      <li> ${schema}
    </#list>
    </ul>
    </div>
    <div>
    Document facets:
    <ul>
    <#list Document.facets as facet>
      <li> ${facet}
    </#list>
    </ul>
    </div>
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
