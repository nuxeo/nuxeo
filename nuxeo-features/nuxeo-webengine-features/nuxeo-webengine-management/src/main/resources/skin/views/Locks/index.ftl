<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="content">

<dl><h2>Locks</h2>
<#list This.infos as info>
 <span class="listing">
   <dt class="item"><a href="${This.path}/${info.resource}">${info.resource}</a></dt><dd><span class="is locked">is locked</span> by 
   <span class="lister">${info.owner}</span> since <span class="dtlisted">${info.lockTime?datetime}</span> and will
   expire at <span class="dtexpired">${info.expiredTime}</span>.</dd>
</#list>
</dl>

</@block>


</@extends>