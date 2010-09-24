<@extends src="base.ftl">

<#assign info=This.info >

<@block name="content">
<h2>Queue info</h2>
${This.name}
<p class="info">
is owned by ${info.ownerName} and is ${info.state}.</p>
</@block>

<@block name="toolbox">
<ul>
 <h3>Toolbox</h3>
 <li><a href="${This.path}/@retry">Retry</a></li>
 <li><a href="${This.path}/@blacklist">Blacklist</a></li>
</ul>
</@block>

</@extends>