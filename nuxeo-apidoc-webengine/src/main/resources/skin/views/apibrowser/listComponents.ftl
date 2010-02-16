<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed components (${componentIds?size})</h1>

<#list componentIds as componentId>

  <A href="${Root.path}/${distId}/viewComponent/${componentId}">${componentId}</A><br/>

</#list>

</@block>

</@extends>