<@extends src="/default/Blog/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#wikipage-actions > ul").tabs();
});
</script>

<div id="message">${request.getParameter('msg')}</div>

<div id="wikipage-actions">
  <ul>
    <li><a href="${this.docURL}@@view_content" title="page_content"><span>View</span></a></li>
    <li><a href="${this.docURL}@@edit" title="edit"><span>Edit</span></a></li>
    <li><a href="${this.docURL}@@show_versions" title="history"><span>History</span></a></li>
  </ul>
  <div id="page_content">
      <h1>${this.title}</h1>

      <@transform name="wiki">${this.blogPost.content}</@transform>
  </div>
</div>
</@block>
</@extends>
