<@extends src="base.ftl">

<@block name="content">

   <p><h3>For server ${serverInstanceId} </h3>
    the administrative status is <emph>${administrativeStatus}</emph></p>

</@block>

<@block name="toolbox">
<ul><h3>Toolbox</h3>
<#if administrativeStatus == 'passive'>
<li><a href="${This.path}/@activate">Activate</a> this server</li>
<#else>
<li><a href="${This.path}/@passivate">Passivate</a> this server</li>
</#if>
</ul>
</@block>

</@extends>