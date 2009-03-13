<#macro contextual>

<table>
  <tr><th colspan="2">${Context.getMessage("title.contextual.links.pages")}</th></tr>
  <#list contextualLinks as cl>
  <tr>
    <td>
      <table>
        <tr><td><img src="${skinPath}/images/contextual_link.gif"/>${cl.description}</td></tr>
        <tr><td><a href="${cl.link}" target="_blank"> ${cl.name} &nbsp; </a></td></tr>
      </table>
    </td>
  </tr>
  </#list>
</table>
</#macro>