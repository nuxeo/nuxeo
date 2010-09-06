<@extends src="base.ftl">

<#assign item=This.item content=This.item.handledContent>

<@block name="content">
<h2>Queue item</h2>
<p class="item"><a href="${This.name}">${content.name}</a> queue item
    <#if content.comments??> with comments "<span class="with comments">${content.comments}</span>"</#if>
    <#if item.orphaned><span class="is orphaned">is orphaned</span></#if></p>
</@block>

<@block name="toolbox">
<#if item.orphaned>
<ul>
 <h3>Toolbox</h3>
 <li><a href="/@retry">Retry</a></li>
 <li><a href="/@cancel">Cancel</a></li>
</ul>
</#if>
</@block>

</@extends>