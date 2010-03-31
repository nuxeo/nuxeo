<#macro viewExtensionPoint extensionPointWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign extensionPointItem=extensionPointWO.getNxArtifact()/>
  <#assign extensionPointDocs=extensionPointWO.getAssociatedDocuments()/>
  <#assign extensionPointDesc=extensionPointDocs.getDescription(Context.getCoreSession())/>

  <div id="extensionPoint.${extensionPointItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
  <A name="extensionPoint.${extensionPointItem.id}"> </A>

  <div class="blocTitle bTitle${nestedLevel}" id="extensionPoint.${extensionPointItem.id}">  ExtensionPoint ${extensionPointDesc.title} (${extensionPointItem.name})
  <A href="${Root.path}/${distId}/viewExtensionPoint/${extensionPointItem.id}/doc"> Edit </A>
  </div>

  <div class="foldablePannel">

  <span class="componentId">Component Id : ${componentItem.id}</span> <br/>

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
    <li>From <A href="${Root.path}/${distId}/viewComponent/${contrib.targetComponentName.name}"> ${contrib.targetComponentName.name}</A> contribution : <A href="${Root.path}/${distId}/viewContribution/${contrib.id}"> ${contrib.id} </A></li>
  </#list>
  </ul>
  </p>

  </div>
  </div>

 <#assign nestedLevel=nestedLevel-1/>
</#macro>


