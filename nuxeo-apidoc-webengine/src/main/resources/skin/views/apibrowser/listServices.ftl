<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed services (${serviceIds?size})</h1>

<#list serviceIds as serviceId>

  <A href="${Root.path}/${distId}/viewService/${serviceId}">${serviceId}</A><br/>

</#list>

</@block>

</@extends>