<h1>${This.title}</h1>

<div id="wiki-content">
    <@transform name="wiki">${This.wikiPage.content}</@transform>
<div>
    <hr/>
<#include "/default/includes/attached_files.ftl">