<@extends src="base.ftl">
<#setting url_escaping_charset="UTF-8">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<h1> listing all deployed Contributions (${cIds?size})</h1>

<#list cIds as cId>

  <A href="${Root.path}/${distId}/viewContribution/${cId?url}">${cId}</A><br/>

</#list>

</@block>

</@extends>