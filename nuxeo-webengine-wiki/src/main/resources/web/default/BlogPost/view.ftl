<@extends src="/default/Blog/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
});
</script>

<div id="message">${Request.getParameter('msg')}</div>

<div id="entry-actions">
  <ul>
    <li><a href="${This.urlPath}@@view_content" title="page_content"><span>View</span></a></li>
    <li><a href="${This.urlPath}@@edit" title="edit"><span>Edit</span></a></li>
    <li><a href="${This.urlPath}@@show_versions" title="history"><span>History</span></a></li>
  </ul>
  <div id="page_content">
      <h1>${This.title}</h1>

      <@transform name="wiki">${Document.blogPost.content}</@transform>
  </div>
</div>
</@block>
</@extends>
