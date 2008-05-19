<@extends src="Wiki/base.ftl">
<@block name="content">

<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
  $("#entry-actions > ul").tabs("select", '#view_content');
});
</script>

<div id="message">${Request.getParameter('msg')}</div>

<div id="entry-actions">
<ul>
  <#list Context.getActions("tabview")?sort as action>
    <li><a href="${This.urlPath}@@${action.id}"  title="${action.id}"><span>${message('action.' + action.id)}</span></a></li>
  </#list>
</ul>

<div id="view_content">
  <#include "WikiPage/view_content.ftl"/>
</div>

</div>

</@block>
</@extends>
