<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm cIds?size 'Contribution'/>

<#list cIds as cId>

  <A href="${Root.path}/${distId}/viewContribution/${cId}">${cId}</A><br/>

</#list>

</@block>

</@extends>