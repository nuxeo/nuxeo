<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="left">
 
 <form method="POST" action="${This.path}" accept-charset="utf-8">
        <input type="submit" class="button" value="Run" />
</form>

 <div class="index_probe">
    <#assign probe=This.info/>
    <h2>Probe <a href="${This.path}">${probe.shortcutName}</a></h2>
    <p>last execution was a <emph><#if probe.inError>failure<#else>success</#if></emph></p>
    <p>probes was executed ${probe.runnedCount} times with a last duration of ${probe.lastDuration} milliseconds.</p>
    <p>${probe.status.info}</p>
 </div>
 
</@block>

</@extends>