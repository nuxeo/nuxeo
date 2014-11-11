<@extends src="base.ftl">

<@block name="content">
 
 <div class="index_probe">
    <#assign probe=This.info/>
    <h2>Probe <a href="${This.path}">${probe.shortcutName}</a></h2>
    <p>last execution was a <emph><#if probe.inError>failure<#else>success</#if></emph></p>
    <p>probes was executed ${probe.runnedCount} times with a last duration of ${probe.lastDuration} milliseconds.</p>
    <p>${probe.status.asXML}</p>
 </div>
 
</@block>

<@block name="toolbox">
<ul><h3>Toolbox</h3>
  <li><a href="${This.path}/@run">Run</a></li>
</ul>
</@block>

</@extends>