<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1> View for Nuxeo Seam Component <span class="componentTitle">${nxItem.id}</span></h1>

 <#assign seamComponent=This.getNxArtifact()/>
 <#assign componentDocs=This.getAssociatedDocuments()/>
 <#assign componentDesc=componentDocs.getDescription(Context.getCoreSession())/>

 <#assign description=docs.getDescription(Context.getCoreSession())/>

 <#include "/views/seamComponent/viewSimple.ftl">

  <p><@docContent docItem=componentDesc /></p>
</@block>

</@extends>