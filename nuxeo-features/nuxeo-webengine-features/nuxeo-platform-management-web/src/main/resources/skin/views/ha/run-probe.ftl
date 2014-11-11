<@extends src="baseProbe.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">
 
  <#if probe_status>
    
    <h3>Probe ${probe.shortcutName}: </h3>
    succeded with status: <p>${probe.probeStatus.status} </p> 
    and run in ${probe.lastDuration} milliseconds.
   <#else>
   		Probe ${probe.shortcutName} failed.
   </#if>

</@block>

</@extends>