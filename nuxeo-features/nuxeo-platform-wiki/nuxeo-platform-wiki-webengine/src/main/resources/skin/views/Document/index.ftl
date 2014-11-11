<@extends src="base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs({
    select: function(item) {
        setContextData("tab", item.tab.title);
    }
  });
  //$("#entry-actions > ul").tabs("select", '#view_content');
});
</script>

<div id="entry-actions">
<ul>
  <#list This.getLinks("TABVIEW") as link>
    <li><a href="${link.getCode(This)}?context=tab" title="${link.id}"><span>${Context.getMessage(link.id)}</span></a></li>
  </#list>
</ul>

</div>

</@block>
</@extends>