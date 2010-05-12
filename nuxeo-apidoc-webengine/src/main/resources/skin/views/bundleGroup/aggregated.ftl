<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<H1> View Bundle group ${nxItem.id}</H1>

<#include "/views/bundleGroup/bundleGroupMacros.ftl">

<@viewBundleGroup bundleGroupWO=This />

</@block>

</@extends>