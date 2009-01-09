<@extends src="Wiki/base.ftl">
<@block name="content">

<div class="summary-entries">
<#list Document.children?reverse as entry>
  <div class="summary-entry">  
    <h2 class="summary-title"><a href="${Root.urlPath}/${entry.name}">${entry.title}</a></h2>
    <div class="summary-content">
        <@wiki>${entry.wikiPage.content}</@wiki>
    </div>
  </div>
  <div class="summary-byline">${entry.dublincore.modified?datetime} by ${entry.dublincore.creator}</div>
</#list>
</div>

</@block>
</@extends>
