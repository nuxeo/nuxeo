<div>
  <#list This.getLinks("SITE_ACTIONS") as link>
    <form method="POST" action="${This.path}/@perspective/${link.path}" accept-charset="utf-8">
      <input type="submit" class="button" value="${Context.getMessage(link.id)}" />
    </form>
  </#list>
</div>
