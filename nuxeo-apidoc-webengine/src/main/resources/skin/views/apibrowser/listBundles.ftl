<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm bundleIds?size 'Bundle'/>

<#list bundleIds as bundleId>

  <A href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</A><br/>

</#list>

</@block>

</@extends>