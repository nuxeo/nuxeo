<@extends src="base.ftl">

  <@block name="title">
      ${collection} ${category}
  </@block>

  <@block name="content">
    <h1>Preset: ${collection} ${category}
      <a style="float: right" href="${Root.getPath()}/${bank}/${collection}/preset/${category}/view">Refresh</a>
    </h1>
    <table class="properties">
    <#list properties?keys as key>
      <tr<#if key_index%2=1> class="odd"</#if>>
        <#assign value=properties[key] />
        <td class="key">${key}</td>
        <td class="preview">
          <#if category='color'>
            <div style="padding: 6px; background-color: ${value}"></div>
          </#if>
          <#if category='background'>
            <div style="padding: 6px; background: ${value}"></div>
          </#if>
          <#if category='border'>
            <div style="padding: 6px; border: ${value}"></div>
          </#if>
          <#if category='font'>
            <div style="font: ${value}">ABC abc</div>
          </#if>
        </td>
        <td class="value">${value}</td>
      </tr>
    </#list>
    </table>
  </@block>

</@extends>