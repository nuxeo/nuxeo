<@extends src="/default/Wiki/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
});
</script>

<!-- TS: JQuery-needed : actions in tabs, this.title under the tabs and content under this.title
    EB: DONE
-->

<div id="message">${request.getParameter('msg')}</div>

<div id="entry-actions">
  <ul>
    <li><a href="${this.docURL}@@view_content" title="page_content"><span>View</span></a></li>
    <li><a href="${this.docURL}@@edit" title="edit"><span>Edit</span></a></li>
    <li><a href="${this.docURL}@@show_versions" title="history"><span>History</span></a></li>
  </ul>
  <div id="page_content">
      <h1>${this.title}</h1>

      <@transform name="wiki">${this.wikiPage.content}</@transform>
  </div>
</div>
</@block>
</@extends>
