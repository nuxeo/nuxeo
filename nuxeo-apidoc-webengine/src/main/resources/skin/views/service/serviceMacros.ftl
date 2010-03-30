<#macro viewService serviceWO >

  <#assign serviceItem=serviceWO.getNxArtifact()/>
  <#assign serviceDocs=serviceWO.getAssociatedDocuments()/>
  <#assign serviceDesc=serviceDocs.getDescription(Context.getCoreSession())/>

  <h2> <A name="service.${serviceItem.id}">${serviceDesc.title} </A></h2>

  <p><@docContent docItem=serviceDesc /></p>


</#macro>


