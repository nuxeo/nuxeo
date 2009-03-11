<#macro pageLastPublished>

<table border="1">
  <tr><th colspan="2">${Context.getMessage("title.last.published.pages")}</th></tr>
  <#list pages as p>
  <tr>
    <td>
      <table border="1">
        <tr><td>${p.day}</td></tr>
        <tr><td>${p.month}</td></tr>
      </table>
    </td>
    <td>
      <table border="1">
        <tr><td><a href="${This.path}/${p.path}"> ${p.name} &nbsp; </a></td></tr>
        <tr><td>${p.author}&nbsp;</td></tr>
      </table>
    </td>
  </tr>
  </#list>
</table>

</#macro>
