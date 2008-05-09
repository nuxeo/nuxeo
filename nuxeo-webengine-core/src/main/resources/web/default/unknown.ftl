<h3>Unknown Page</h3>

<#if context.hasUnresolvedObjects()>

<#assign name = context.getFirstUnresolvedSegment()>

The document ${name} doesn't exist. Click
<a href="${this.absolutePath}/${name}@@create_form">here</a>
if you want to create a new document.

<#else>


The page you requested doesn't exists

</#if>

