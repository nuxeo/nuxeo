<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<H1> View for ${nxItem.artifactType} ${nxItem.id}</H1>
<#assign description=docs.getDescription(Context.getCoreSession())/>

<h2> ${description.title} </h2>

<p>${description.content}</p>

${description.title} is composed of


</@block>

</@extends>