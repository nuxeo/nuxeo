<@extends src="base.ftl">


<@block name="content">

<dl><h2>${This.manager.name.schemeSpecificPart} queue</h2>
<#list This.infos as info>
 <span class="listing">
      <dt><a href="${This.name}/${info.name}">${info.name}</a></dt>
       <dd>with comments "<span class="with comment">${info.handledContent.comments}</span>
             <#if info.orphaned><span class="is orphaned">is orphaned</span></#if></dd>
</#list>
</dl>

</@block>

<@block name="toolbox">
<ul><h3>Toolbox</h3>
<li><a href="${This.path}/@cancel">cancel</a></li>
<li><a href="${This.path}/@retry">retry</a></li>
</ul>
</@block>

</@extends>