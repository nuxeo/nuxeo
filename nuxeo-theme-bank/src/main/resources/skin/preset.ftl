<@extends src="base.ftl">

  <@block name="title">
      ${collection} ${category}
  </@block>

  <@block name="content">
    <h1><a href="${Root.getPath()}/${bank}/preset/view">preset</a> <a href="${Root.getPath()}/${bank}/preset/${collection}/view">${collection}</a> &gt; ${category}</h1>
    <table class="properties">
    <#list properties?keys as key>
      <tr<#if key_index%2=1> class="odd"</#if>>
        <td class="key">${key}</td>
        <td class="value">${properties[key]}</td>
      </tr> 
    </#list>
    </table>
  </@block>

</@extends>