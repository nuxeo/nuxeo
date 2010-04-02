<#macro viewExtensionPoint extensionPointWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign extensionPointItem=extensionPointWO.getNxArtifact()/>
  <#assign extensionPointDocs=extensionPointWO.getAssociatedDocuments()/>
  <#assign extensionPointDesc=extensionPointDocs.getDescription(Context.getCoreSession())/>

  <div id="extensionPoint.${extensionPointItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
  <A name="extensionPoint.${extensionPointItem.id}"> </A>

  <div class="blocTitle bTitle${nestedLevel}" id="extensionPoint.${extensionPointItem.id}">
  <img src="${skinPath}/images/${extensionPointDesc.targetType}.png" alt="ExtensionPoint"/>
   ${extensionPointDesc.title} (${extensionPointItem.name})
  <A href="${Root.path}/${distId}/viewExtensionPoint/${extensionPointItem.id}/doc"> Edit </A>
  </div>

  <div class="foldablePannel">

  <span class="componentId">ExtensionPoint Id : ${extensionPointItem.id}</span> <br/>

  <p><@docContent docItem=extensionPointDesc /></p>

  <span class="resourceToggle"> Built-in documentation</span> for this extension point.
  <div class="hiddenResource">
  <pre>
  <code>
  ${extensionPointItem.documentation?html}
  </code>
  </pre>
  </div>

  <br/>

  <p>
  <h4> Known contributions </h4>
  <ul>
  <#list extensionPointItem.extensions as contrib>
    <li><A href="${Root.path}/${distId}/viewContribution/${contrib.id}"> ${contrib.id} </A></li>
  </#list>
  </ul>
  </p>

  <@viewAdditionnalDoc docsByCat=extensionPointDocs.getDocumentationItems(Context.getCoreSession())/>
  </div>
  </div>

 <#assign nestedLevel=nestedLevel-1/>
</#macro>


