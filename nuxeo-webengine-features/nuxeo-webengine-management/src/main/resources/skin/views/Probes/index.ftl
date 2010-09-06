<@extends src="base.ftl">

<@block name="content">

<dl class="list probes"><h2>Probes</2>
<form method="POST" 
  action="${This.path}" accept-charset="utf-8">
        <input type="submit" class="button" value="Run" />&nbsp;
</form>  
<#list This.infos as probe>
 <#assign status = probe.status/>
  <span class="probe ${probe.shortcutName}">
    <dt><a href="${This.path}/${probe.shortcutName}">${probe.shortcutName}</a></dt>
    <dd>
    <p>last execution was a <span class="last status"><#if probe.inError>failure</span><#else>success</#if></span></p>
    <p>probes was executed  ${probe.runnedCount} times with a last duration of ${probe.lastDuration} milliseconds.</p>
    <p><span class="last info">${status.info}</span></p>
    </dd>
  </span>
</#list> 

</@block>

</@extends>