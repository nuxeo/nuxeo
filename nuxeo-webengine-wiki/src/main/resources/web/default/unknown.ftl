<@extends src="/default/base.ftl">
<@block name="content">

<h3>Unknown Page</h3>

<#if Context.hasUnresolvedObjects()>

<#assign name = Context.getFirstUnresolvedSegment()>

The document ${name} doesn't exist. Click
<a href="${Root.urlPath}/${name}@@create_entry">here</a>
if you want to create a new document.

<#else>


The page you requested doesn't exists

</#if>

</@block>
</@extends>

