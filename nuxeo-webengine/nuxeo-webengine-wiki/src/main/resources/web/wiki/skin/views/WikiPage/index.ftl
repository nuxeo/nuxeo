<@extends src="base.ftl">
<@block name="content">

<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs({
    select: function(item) {
        setContextData("tab", item.tab.title);
    }
  });
});
</script>

<!-- Commented : request not known. message instead
div id="message">$ { Request.getParameter('msg')}</div-->
<div id="message"> </div>

<div id="entry-actions">
<ul>
  <#list This.getLinks("TABVIEW") as link>
    <li><a href="${link.getCode(This)}?context=tab" title="${link.id}"><span>${Context.getMessage(link.id)}</span></a></li>
  </#list>
</ul>

</div>

</@block>
</@extends>
