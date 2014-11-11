<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
 
 <form method="POST" 
  action="${This.path}" accept-charset="utf-8">
        <input type="submit" class="button" value="Run" />
</form> 

 <div class="index_probe">
    <#assign status = probe.status/>
    <h3><a href="${This.path}">${probe.shortcutName}</a></h3>
    <p>last execution was a <emph><#if probe.inError>failure<#else>success</#if></emph></p>
    <p>probes was executed ${probe.runnedCount} times with a last duration of ${probe.lastDuration} milliseconds.</p>
    <p>${status.info}</p>
 </div>
 
</@block>

</@extends>