<@extends src="base.ftl">

<#assign info=This.info >

<@block name="content">
<h2>Queue info</h2>
<p class="info"><a href="${This.name.fragment}">${info.name.fragment}</a> is owned by ${This.ownerName}
    <#if info.orphaned><span class="is orphaned"> and is orphaned</span></#if></p>
</@block>

<@block name="toolbox">
<ul>
 <h3>Toolbox</h3>
 <li><a href="${This.path}/@retry">Retry</a></li>
 <li><a href="${This.path}/@cancel">Cancel</a></li>
</ul>
</@block>

</@extends>