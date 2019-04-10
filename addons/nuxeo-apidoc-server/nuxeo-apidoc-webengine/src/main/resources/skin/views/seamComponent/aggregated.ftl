<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">

<@block name="stylesheets"></@block>
<@block name="header_scripts"></@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1> View for Nuxeo Seam Component <span class="componentTitle">${nxItem.id}</span></h1>

<div class="tabscontent">

   <#assign seamComponent=This.getNxArtifact()/>
   <#assign componentDocs=This.getAssociatedDocuments()/>
   <#assign componentDesc=componentDocs.getDescription(Context.getCoreSession())/>

   <#assign description=docs.getDescription(Context.getCoreSession())/>

  <div id="SeamComponent.${seamComponent.id}_frame" class="blocFrame">

   <span id="${componentDesc.getEditId()}_doctitle"> ${componentDesc.title}</span>
   <@quickEditorLinks docItem=componentDesc/>

   <p><@docContent docItem=componentDesc /></p>

   <#include "/views/seamComponent/viewSimple.ftl">

   <@viewAdditionalDoc docsByCat=componentDocs.getDocumentationItems(Context.getCoreSession())/>

  </div>

</div>

</@block>

</@extends>