<#import "common/util.ftl" as base/>
<h1>${Document.title}</h1>

<div id="entry-content">
    <@transform name="wiki">${Document.wikiPage.content}</@transform>
<div>

<div id="entry-attachments">
  <#include "includes/attached_files.ftl">
</div>  