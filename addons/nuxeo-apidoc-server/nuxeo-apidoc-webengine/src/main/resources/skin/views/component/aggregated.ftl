<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Component <span class="componentTitle">${nxItem.id}</span>
  <a href="${Root.path}/${distId}/viewBundle/${nxItem.bundle.id}" title="Go to parent bundle">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

<#assign description=docs.getDescription(Context.getCoreSession())/>

<#include "/views/component/macros.ftl">

<@viewComponent componentWO=This />
</@block>

</@extends>