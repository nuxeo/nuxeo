<@extends src="base.ftl">

<@block name="content">
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
</@block>

</@extends>
