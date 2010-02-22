<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed Contributions (${cIds?size})</h1>

<#list cIds as cId>

  <A href="${Root.path}/${distId}/viewContribution/${cId}">${cId}</A><br/>

</#list>

</@block>

</@extends>