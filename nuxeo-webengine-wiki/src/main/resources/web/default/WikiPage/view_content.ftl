<h1>${this.title}</h1>

<div id="wiki-content">
    <@transform name="wiki">${this.wikiPage.content}</@transform>
<div>
    <hr/>
<#include "/default/includes/attached_files.ftl">