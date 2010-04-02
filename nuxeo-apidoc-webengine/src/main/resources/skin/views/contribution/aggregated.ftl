<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<H1> View for ${nxItem.artifactType} ${nxItem.id}
<A href="${Root.path}/${distId}/viewComponent/${This.nxArtifact.id?split("--")[0]}/aggView"> Up </A>
</H1>

<#include "/views/contribution/contributionMacros.ftl">

<@viewContribution contributionWO=This />

</@block>

</@extends>