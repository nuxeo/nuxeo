<@extends src="Blog/base.ftl">

<@block name="content">
<div class="summary-entries">
<#list This.document.children?reverse as entry>
  <div class="summary-entry">  
    <h2 class="summary-title"><a href="${Context.getUrlPath(entry)}">${entry['dc:title']}</a></h2>
    <div class="summary-content">
        ${entry['bp:content']}
  </div>
  <div class="summary-byline">${entry["dc:modified"]?datetime} by ${entry['dc:creator']}</div>
 </div>
</#list>
</div>

</@block>
</@extends>
