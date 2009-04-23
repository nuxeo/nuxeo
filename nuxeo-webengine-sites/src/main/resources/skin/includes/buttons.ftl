<#macro buttons>

  <div>
    <#list This.getLinks("SITE_ACTIONS") as link>
      <form method="POST" action="${This.path}/${link.path}" accept-charset="utf-8">
        <input type="submit" value="${Context.getMessage(link.id)}" />
      </form>
    </#list>
  </div>

</#macro> 