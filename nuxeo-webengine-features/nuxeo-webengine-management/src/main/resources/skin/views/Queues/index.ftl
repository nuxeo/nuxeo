<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="left">

<dl><h2>Queues</h2>
<#list This.queues as queue>
 <span class="listing">
   <dt class="item"><a href="${This.path}/${queue.name}">${queue.name}</a></dt><dd><span class="is handling ">is handling</span> 
   <span class="handled count">${queue.listHandledItems()?size}</span> items
   <#if queue.listOrphanedItems()?size > and has orphaned content</#if>.</dd>
</#list>
</dl>

</@block>

</@extends>