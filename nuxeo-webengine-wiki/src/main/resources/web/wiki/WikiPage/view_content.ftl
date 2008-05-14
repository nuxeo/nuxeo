<h1>${Document.title}</h1>

<div id="wiki-content">
    <@transform name="wiki">${Document.wikiPage.content}</@transform>
<div>
    <hr/>
<#include "includes/attached_files.ftl">