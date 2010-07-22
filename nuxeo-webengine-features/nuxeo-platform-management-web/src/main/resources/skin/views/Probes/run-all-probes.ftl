<@extends src="baseProbe.ftl">

<@block name="stylesheets">
<style>
</style>
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h2>Probe status: </h2>
 <#list probes_in_error as item>
    <div class="index_item">Probe ${item}: failed.</div>
  </#list>
  
  <#list probes_succeded as item>
  
    <div class="index_item"><h3>Probe ${item.shortcutName}: </h3>
    succeded with status: <p>${item.probeStatus.status} </p> </div>
    and run in ${item.lastDuration} milliseconds.
  </#list> 

</@block>

</@extends>