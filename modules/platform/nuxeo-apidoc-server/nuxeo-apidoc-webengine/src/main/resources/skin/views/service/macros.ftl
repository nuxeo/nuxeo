<#macro viewService serviceWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign serviceItem=serviceWO.getNxArtifact()/>
  <#assign serviceDocs=serviceWO.getAssociatedDocuments()/>
  <#assign serviceDesc=serviceDocs.getDescription(Context.getCoreSession())/>

  <div id="Service.${serviceItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
    <a name="Service.${serviceItem.id}"> </a>

    <div class="blocTitle bTitle${nestedLevel}" id="Service.${serviceItem.id}">
      <img src="${skinPath}/images/${serviceDesc.targetType}.png" alt="Service"/>
      <span id="${serviceDesc.getEditId()}_doctitle"> ${serviceDesc.title}</span>

      <@quickEditorLinks docItem=serviceDesc/>

      <#if This.nxArtifact.id!=serviceItem.id>
        &nbsp;&nbsp;
        <a href="${Root.path}/${distId}/viewService/${serviceItem.id}/">
          <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
        </a>
      </#if>
    </div>

    <div class="foldablePanel">
      <span class="componentId">Service Id : ${serviceItem.id}</span> <br/>

      <p><@docContent docItem=serviceDesc /></p>

      <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(serviceItem.id)}"/>
      <#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${serviceItem.id?replace('.','/')}.html"/>

      Associated JavaDoc
      <div>
        <a href="${javaDocUrl}" target="NxJavaDoc">Open in a new window</a>
        <iframe src="${javaDocUrl}" width="98%" height="300px" border="0"></iframe>
      </div>

      <@viewAdditionalDoc docsByCat=serviceDocs.getDocumentationItems(Context.getCoreSession())/>

    </div>
  </div>

  <#assign nestedLevel=nestedLevel-1 />

</#macro>
