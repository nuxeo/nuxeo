<@extends src="/default/Wiki/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#wikipage-actions > ul").tabs();
});
</script>

<!-- TS: JQuery-needed : actions in tabs, this.title under the tabs and content under this.title
    EB: DONE
    -->

<@block name="message"/>

<div id="wikipage-actions">
  <ul>
    <li><a href="${this.docURL}@@view_content" title="view"><span>View</span></a></li>
    <li><a href="${this.docURL}@@edit" title="edit"><span>Edit</span></a></li>
    <li><a href="${this.docURL}@@show_versions" title="history"><span>History</span></a></li>
  </ul>
</div>
</@block>
</@extends>
