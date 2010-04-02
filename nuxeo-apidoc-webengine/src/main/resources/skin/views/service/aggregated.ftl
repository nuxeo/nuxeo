<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<H1> View for ${nxItem.artifactType} ${nxItem.id}</H1>

<#include "/views/service/serviceMacros.ftl">

<@viewService serviceWO=This />

</@block>

</@extends>