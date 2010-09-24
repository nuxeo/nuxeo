<@extends src="base.ftl">


<@block name="content">

<dl><h2>${This.manager.name.schemeSpecificPart} queue</h2>
<#list This.infos as info>
 <span class="listing">
      <dt><a href="${This.name}/${info.name.fragment}">${info.name}</a></dt>
       <dd><#if  info.lastHandlingDate??> was handled at  <span class="was handled">${info.lastHandlingDate?datetime}</span></if>
       </#if> and <span class="is in state">is ${info.state}</span>.  </dd>
</#list>
</dl>

</@block>

<@block name="toolbox">
<ul><h3>Toolbox</h3>
<li><a href="${This.path}/@blacklist">blacklist</a></li>
<li><a href="${This.path}/@retry">retry</a></li>
</ul>
</@block>

</@extends>