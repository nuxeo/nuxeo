<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="left">

<#assign item=This.item>

 <form method="POST" action="${This.path}/@cancel" accept-charset="utf-8">
        <input type="submit" class="button" value="Cancel" />
</form>

 <form method="POST" action="${This.path}/@retry" accept-charset="utf-8">
        <input type="submit" class="button" value="Retry" />
</form>

<p class="item"><a href="${This.name}/${item.handledContent.name}">${item.handledContent.name}</a> queue item
    with comments "<span class="with comment">${item.handledContent.comments}</span>"
             <#if item.orphaned><span class="is orphaned">is orphaned</span></#if></p>

</@block>

</@extends>