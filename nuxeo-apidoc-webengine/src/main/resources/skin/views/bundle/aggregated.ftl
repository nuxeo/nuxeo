<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<H1> View Nuxeo Bundle <span class="componentTitle">${nxItem.id}</span></H1>
<#assign description=docs.getDescription(Context.getCoreSession())/>

<#include "/views/bundle/bundleMacros.ftl">

<@viewBundle bundleWO=This />

</@block>

</@extends>