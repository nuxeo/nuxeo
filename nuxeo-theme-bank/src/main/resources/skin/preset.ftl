<@extends src="base.ftl">

  <@block name="title">
      <title>${resource}</title>
  </@block>

  <@block name="content">
    <h1>${resource}</h1>
    <table style="width: 100%" cellpadding="1">
    <#list properties?keys as key>
      <tr>
        <td style="width: 25%">${key}</td>
        <td style="width: 75%">${properties[key]}</td>
      </tr> 
    </#list>
    </table>
  </@block>

</@extends>
