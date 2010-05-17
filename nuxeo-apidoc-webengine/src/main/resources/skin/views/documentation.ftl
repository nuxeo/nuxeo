<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="stylesheets">
</@block>

<@block name="header_scripts">
</@block>

<@block name="right">
<H1> Documentation for ${nxItem.id}</H1>

<#if Root.isEditor()>
<A href="${This.path}/createForm"> Add new </A>
</#if>
<br/>

<#assign docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>
<#assign docsByCat2=docs.getDocumentationItems(Context.getCoreSession())/>

<h2> Documentation index </h2>
<#list docsByCat?keys as category>
 <h3>${category}</h3>
 <ul>
 <#list docsByCat2[category] as docItem>
    <li><A href="#${docItem.id}">${docItem.title}</A> </li>
 </#list>
 </ul>
</#list>

<hr>

<#list docsByCat?keys as category>
 <h2>${category}</h2>

 <#list docsByCat2[category] as docItem>
    <A name="${docItem.id}"> ${docItem.title} </A> &nbsp;
    <#if Root.isEditor()>
      [ <A href="${This.path}/editForm/${docItem.getUUID()}">Edit</A> ]
    </#if>
    <#include "docItemView.ftl">
 </#list>
</#list>

</@block>

</@extends>