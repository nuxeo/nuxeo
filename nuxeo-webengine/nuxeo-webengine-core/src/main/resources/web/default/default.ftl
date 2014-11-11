<@extends src="base.ftl">
<@block name="content">

<h1>404 - Resource Not Found</h1>

<p>
The page you requested doesn't exists.
</p>

<#if Session.hasPermission(Document.ref, "Write")>
<#if Context.pathInfo.hasTrailingPath()>
<#assign name = Context.getFirstUnresolvedSegment()>
<p>
Click <a href="${This.urlPath}/${name}@@create_entry">here</a>
if you want to create a new document named ${name}.
</p>
</#if>
</#if>

</@block>
</@extends>

