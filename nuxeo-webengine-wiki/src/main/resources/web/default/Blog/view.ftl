<@extends src="/default/Blog/base.ftl">
<@block name="content">
<div class="summary-entries">
<#list this.children?reverse as entry>
    <h2 class="summary-title"><a href="${entry.name}">${entry.title}</a></h2>
    <div class="summary-content">
        ${entry.blogPost.content}
    </div>
    <div class="summary-byline">${entry.dublincore.modified?datetime} by ${entry.dublincore.creator}</div>
    <#if entry_index = 15><#break></#if>
</#list>
</div>

</@block>
</@extends>