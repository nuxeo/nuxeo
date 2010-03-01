<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed services (${services?size})</h1>

<#list services as service>

  <A href="${Root.path}/${distId}/viewService/${service.id}">${service.label}</A><br/>

</#list>

</@block>

</@extends>