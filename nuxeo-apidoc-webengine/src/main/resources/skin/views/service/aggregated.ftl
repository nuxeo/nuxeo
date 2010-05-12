<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<H1> View Service <span class="componentTitle">${nxItem.id}</span>
  <A shref="${Root.path}/${distId}/viewComponent/" title="go to parent component"> <img src="${skinPath}/images/up.gif"/> </A>

</H1>

<#include "/views/service/serviceMacros.ftl">

<@viewService serviceWO=This />

</@block>

</@extends>