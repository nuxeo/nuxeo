<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<H1> View for ${nxItem.artifactType} ${nxItem.id}</H1>
<#assign description=docs.getDescription(Context.getCoreSession())/>

<#include "/views/bundle/bundleMacros.ftl">

<@viewBundle bundleWO=This />

</@block>

</@extends>