<@extends src="Wiki/base.ftl">
<@block name="content">

<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
  $("#entry-actions > ul").tabs("select", '#content_page');
});
</script>

<div id="message">${Request.getParameter('msg')}</div>

<div id="entry-actions">
<ul>
  <#list Context.getActions("TABVIEW")?sort as action>
    <li><a href="${This.urlPath}@@${action.id}"  title="${action.id}"><span>${message('action.' + action.id)}</span></a></li>
  </#list>
</ul>

<div id="content_page">
  <#include "WikiPage/content_page.ftl"/>
</div>

</div>

</@block>
</@extends>
