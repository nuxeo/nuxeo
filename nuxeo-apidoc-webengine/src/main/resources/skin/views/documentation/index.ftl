<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<H1> Documentation</H1>

<#assign categories=docsByCat?keys/>

<h2> Documentation index </h2>
<#list categories as category>
 <h3>${category}</h3>
 <ul>
 <#list docsByCat[category] as docItem>
    <li><A href="${Root.path}/${distId}/doc/view/${docItem.getUUID()}">${docItem.title}</A> </li>
 </#list>
 </ul>
</#list>

</@block>

</@extends>