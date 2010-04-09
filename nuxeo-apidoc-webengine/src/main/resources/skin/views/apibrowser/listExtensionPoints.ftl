<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
</@block>

<@block name="right">

<#include "/docMacros.ftl">

<@filterForm eps?size 'ExtensionPoint'/>

<#list eps as ep>

  <A href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.label}</A><br/>

</#list>

</@block>

</@extends>