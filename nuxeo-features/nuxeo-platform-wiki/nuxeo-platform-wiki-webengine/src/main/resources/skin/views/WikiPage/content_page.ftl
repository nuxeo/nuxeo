<#import "common/util.ftl" as base/>
<h1>${Document.title}</h1>

<div id="entry-content">
  <@nxwiki>${Document.wikiPage.content}</@nxwiki>
</div>

<div id="entry-attachments">
  <#include "includes/attached_files.ftl">
</div>  

<div id="entry-comments">
  <#include "comments/show_comments.ftl">
</div>
