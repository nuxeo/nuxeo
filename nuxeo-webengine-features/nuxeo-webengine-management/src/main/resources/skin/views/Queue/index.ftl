<@extends src="base.ftl">


<@block name="content">

<dl><h2>${This.manager.name} queue</h2>
<#list This.items as item>
 <span class="listing">
      <dt><a href="${This.name}/${item.handledContent.name}">${item.handledContent.name}</a></dt>
       <dd>with comments "<span class="with comment">${item.handledContent.comments}</span>
             <#if item.orphaned><span class="is orphaned">is orphaned</span></#if></dd>
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