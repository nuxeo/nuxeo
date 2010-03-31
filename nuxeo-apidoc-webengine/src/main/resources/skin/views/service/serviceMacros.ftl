<#macro viewService serviceWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign serviceItem=serviceWO.getNxArtifact()/>
  <#assign serviceDocs=serviceWO.getAssociatedDocuments()/>
  <#assign serviceDesc=serviceDocs.getDescription(Context.getCoreSession())/>

  <div id="Service.${serviceItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
    <A name="Service.${serviceItem.id}"> </A>

    <div class="blocTitle bTitle${nestedLevel}" id="Service.${serviceItem.id}">  Service ${serviceDesc.title}
    <A href="${Root.path}/${distId}/viewService/${serviceItem.id}/doc"> Edit </A>
    </div>

    <div class="foldablePannel">

    <span class="componentId">Service Id : ${serviceItem.id}</span> <br/>

    <p><@docContent docItem=serviceDesc /></p>

  </div>
  </div>

  <#assign nestedLevel=nestedLevel-1 />

</#macro>


