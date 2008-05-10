<@extends src="/default/Wiki/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
});
</script>

<!-- TS: JQuery-needed : actions in tabs, This.title under the tabs and content under This.title
    EB: DONE
-->

<div id="message">${Request.getParameter('msg')}</div>

<div id="entry-actions">
  <ul>
    <li><a href="${This.docURL}@@view_content" title="page_content"><span>View</span></a></li>
    <li><a href="${This.docURL}@@edit" title="edit"><span>Edit</span></a></li>
    <li><a href="${This.docURL}@@show_versions" title="history"><span>History</span></a></li>
  </ul>
  <div id="page_content">
      <h1>${This.title}</h1>
      <@transform name="wiki">${This.wikiPage.content}</@transform>
  </div>
</div>


</@block>
</@extends>
