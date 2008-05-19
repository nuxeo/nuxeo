<#import "common/util.ftl" as base/>
<h1>${Document.title}</h1>

<div id="wiki-content">
    <@transform name="wiki">${Document.wikiPage.content}</@transform>
</div>

<#include "includes/attached_files.ftl">

<#-- include "comments/show_comments.ftl"-->