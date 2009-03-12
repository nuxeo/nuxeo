<#macro lastPublished>

<table>
  <tr><th colspan="2">${Context.getMessage("title.last.published.pages")}</th></tr>
  <#list pages as p>
  <tr>
    <td>
      <table>
        <tr><td>${p.day}</td></tr>
        <tr><td>${p.month}</td></tr>
      </table>
    </td>
    <td>
      <table>
        <tr><td><a href="${This.path}/${p.path}"> ${p.name} &nbsp; </a></td></tr>
        <tr><td>${p.author}&nbsp;</td></tr>
        <tr><td>${p.numberComments} ${Context.getMessage("last.published.pages.comments")}</td></tr>
      </table>
    </td>
  </tr>
  </#list>
</table>

</#macro>
