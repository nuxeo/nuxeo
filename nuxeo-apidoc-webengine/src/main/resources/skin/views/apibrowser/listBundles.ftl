<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed bundles (${bundleIds?size})</h1>

<#list bundleIds as bundleId>

  <A href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</A><br/>

</#list>

</@block>

</@extends>