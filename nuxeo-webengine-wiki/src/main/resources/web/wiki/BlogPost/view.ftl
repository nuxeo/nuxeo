<@extends src="Blog/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
});
</script>

<div id="message">${Request.getParameter('msg')}</div>

<div id="entry-actions">
  <ul>
  <#list Context.getActions("tabview") as action>
    <li><a href="${This.urlPath}@@${action.id}" title="${action.id}"><span>${message('action.' + action.id)}</span></a></li>
  </#list>
  </ul>
  
  <div id="view_content">
      <h1>${Document["dc:title"]}</h1>
      ${Document["bp:content"]}
  </div>
</div>


</@block>
</@extends>
