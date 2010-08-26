<@extends src="base.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h2>Probes</h2>

<form method="POST" 
  action="${This.path}" accept-charset="utf-8">
        <input type="submit" class="button" value="Run" />&nbsp;
</form>  

<#list probes as probe>
 <#assign status = probe.status/>
 <div class="index_probe">
    <h3><a href="${This.path}/${probe.shortcutName}">${probe.shortcutName}</a></h3>
    <p>last execution was a <emph><#if probe.inError>failure<#else>success</#if></emph></p>
    <p>probes was executed  ${probe.runnedCount} times with a last duration of ${probe.lastDuration} milliseconds.</p>
    <p>${status} ${status.info}</p>
 </div>
</#list> 

</@block>

</@extends>