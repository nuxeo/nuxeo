<@extends src="/default/base.ftl">
<@block name="content">

<h3>Unknown Page</h3>

<#if context.hasUnresolvedObjects()>

<#assign name = context.getFirstUnresolvedSegment()>

The document ${name} doesn't exist. Click
<a href="${This.docURL}/${name}@@create_entry">here</a>
if you want to create a new document.

<#else>


The page you requested doesn't exists

</#if>

</@block>
</@extends>

