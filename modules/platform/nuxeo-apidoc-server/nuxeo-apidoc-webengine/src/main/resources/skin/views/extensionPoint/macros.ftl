<#macro viewExtensionPoint extensionPointWO expand=false>

  <#assign nestedLevel=nestedLevel+1/>
  <#assign extensionPointItem=extensionPointWO.getNxArtifact()/>
  <#assign extensionPointDocs=extensionPointWO.getAssociatedDocuments()/>
  <#assign extensionPointDesc=extensionPointDocs.getDescription(Context.getCoreSession())/>

  <div id="extensionPoint.${extensionPointItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
    <a name="extensionPoint.${extensionPointItem.id}"> </a>

    <div class="blocTitle bTitle${nestedLevel}" id="extensionPoint.${extensionPointItem.id}">
      <img src="${skinPath}/images/${extensionPointDesc.targetType}.png" alt="ExtensionPoint"/>
      <span id="${extensionPointDesc.getEditId()}_doctitle">${extensionPointDesc.title}</span> (${extensionPointItem.name})

      <@quickEditorLinks docItem=extensionPointDesc/>

      <#if This.nxArtifact.id!=extensionPointItem.id>
        &nbsp;&nbsp;
        <a href="${Root.path}/${distId}/viewExtensionPoint/${extensionPointItem.id}/">
          <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
        </a>
      </#if>
    </div>

    <div class="foldablePanel">
      <span class="componentId">ExtensionPoint Id : ${extensionPointItem.id}</span> <br/>

      <p><@docContent docItem=extensionPointDesc /></p>

      <div>
        ${extensionPointItem.documentationHtml}
      </div>

      <br/>
      <br/>
      <p>
        <b>Contributions</b><br/>
        <#if extensionPointItem.extensions?size == 0>
          No known contributions.
        <#else>
        <ul>
        <#list extensionPointItem.extensions as contrib>
          <#if !expand>
            <li><A href="${Root.path}/${distId}/viewContribution/${contrib.id}"> ${contrib.id} </A></li>
          </#if>
          <#if expand>
            <li>
              <p>
                <div>${contrib.id}</div>
                <pre>
                  <code>
                  ${contrib.xml?html}
                  </code>
                </pre>
              </p>
            </li>
          </#if>
        </#list>
        </ul>
        </#if>
      </p>

      <#if !expand>
        <@viewAdditionalDoc docsByCat=extensionPointDocs.getDocumentationItems(Context.getCoreSession())/>
      </#if>

    </div>
  </div>

 <#assign nestedLevel=nestedLevel-1/>
</#macro>
