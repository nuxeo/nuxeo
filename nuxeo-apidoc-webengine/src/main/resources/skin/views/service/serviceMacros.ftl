<#macro viewService serviceWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign serviceItem=serviceWO.getNxArtifact()/>
  <#assign serviceDocs=serviceWO.getAssociatedDocuments()/>
  <#assign serviceDesc=serviceDocs.getDescription(Context.getCoreSession())/>

  <div id="Service.${serviceItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
    <A name="Service.${serviceItem.id}"> </A>

    <div class="blocTitle bTitle${nestedLevel}" id="Service.${serviceItem.id}">
    <img src="${skinPath}/images/${serviceDesc.targetType}.png" alt="Service"/>
     ${serviceDesc.title}

<#if Root.isEditor()>
    <A href="${Root.path}/${distId}/viewService/${serviceItem.id}/doc">
    <img src="${skinPath}/images/edit.png" alt="Edit"/>
    </A>
</#if>
<#if This.nxArtifact.id!=serviceItem.id>
&nbsp;&nbsp;
    <A href="${Root.path}/${distId}/viewService/${serviceItem.id}/">
    <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
    </A>
</#if>
    </div>

    <div class="foldablePannel">

    <span class="componentId">Service Id : ${serviceItem.id}</span> <br/>

    <p><@docContent docItem=serviceDesc /></p>

    <#assign javaDocUrl="http://doc.nuxeo.org/current/apidocs/${serviceItem.id?replace('.','/')}.html"/>

    Associated <span class="resourceToggle"> JavaDoc </span>
    <div class="hiddenResource"><br/>
    <A href="${javaDocUrl}" target="NxJavaDoc">Open in a new window</A>
    <iframe src="${javaDocUrl}" width="98%" height="300px" border="0"></iframe>

   </div>

   <@viewAdditionnalDoc docsByCat=serviceDocs.getDocumentationItems(Context.getCoreSession())/>

  </div>
  </div>

  <#assign nestedLevel=nestedLevel-1 />

</#macro>


