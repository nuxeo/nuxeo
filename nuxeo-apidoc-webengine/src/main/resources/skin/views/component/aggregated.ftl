<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<H1> View for Nuxeo Component <span class="componentTitle">${nxItem.id}</span>
<A href="${Root.path}/${distId}/viewBundle/${This.nxArtifact.bundle.id}/" title="go to parent bundle"> <img src="${skinPath}/images/up.gif"/> </A>

</H1>
<#assign description=docs.getDescription(Context.getCoreSession())/>

<#include "/views/component/componentMacros.ftl">

<@viewComponent componentWO=This />

</@block>

</@extends>