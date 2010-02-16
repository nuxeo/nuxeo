<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed ExtensionPoints (${epIds?size}) </h1>

<#list epIds as epId>

  <A href="${Root.path}/${distId}/viewExtensionPoint/${epId}">${epId}</A><br/>

</#list>

</@block>

</@extends>