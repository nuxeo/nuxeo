<@extends src="/default/Wiki/base.ftl">

<@block name="content">
<div class="summary-entries">
<#list this.children?reverse as entry>
    <h2 class="summary-title"><a href="${entry.name}">${entry.title}</a></h2>
    <div class="summary-content">
        <@transform name="wiki">${entry.wikiPage.content}</@transform>
    </div>
    <div class="summary-byline">${entry.dublincore.modified?datetime} by ${entry.dublincore.creator}</div>
</#list>
</div>

</@block>
</@extends>
