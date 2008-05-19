<h3>Unknown Page</h3>

<#if Context.hasUnresolvedObjects()>

<#assign name = Context.getFirstUnresolvedSegment()>

The document ${name} doesn't exist. Click
<a href="${This.urlPath}/${name}@@create_form">here</a>
if you want to create a new document.

<#else>


The page you requested doesn't exists

</#if>

