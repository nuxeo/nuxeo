<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="left">

 <h2>Lock</h2>

<#assign info=This.info/>

 <form method="POST" action="${This.path}/@unlock" accept-charset="utf-8">
        <input type="submit" class="button" value="Force Unlock" />
</form>

 <p class="listing">
   <span class="item"><a href="${This.path}">${info.resource}</a><span> <span class="is locked">is locked</span> by 
   <span class="lister">${info.owner}</span> since <span class="dtlisted">${info.lockTime?datetime}</span> and will
   expire at <span class="dtexpired">${info.expiredTime}</span>.
 </p>

</@block>


</@extends>