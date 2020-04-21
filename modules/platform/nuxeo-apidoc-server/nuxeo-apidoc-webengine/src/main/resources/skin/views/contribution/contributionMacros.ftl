<#macro viewContribution contributionWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign contributionItem=contributionWO.getNxArtifact()/>
  <#assign contributionDocs=contributionWO.getAssociatedDocuments()/>
  <#assign contributionDesc=contributionDocs.getDescription(Context.getCoreSession())/>

  <div id="contribution.${contributionItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
  <a name="contribution.${contributionItem.id}">  </a>

  <div class="blocTitle bTitle${nestedLevel}" id="contribution.${contributionItem.id}">
  <img src="${skinPath}/images/${contributionDesc.targetType}.png" alt="Contribution"/>
  <span id="${contributionDesc.getEditId()}_doctitle"> ${contributionDesc.title}</span>

  <@quickEditorLinks docItem=contributionDesc />

  <#if This.nxArtifact.id!=contributionItem.id>
    &nbsp;&nbsp;
    <a href="${Root.path}/${distId}/viewContribution/${contributionItem.id}/">
      <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
    </a>
  </#if>

  </div>

  <div class="foldablePanel">

    <span class="componentId">Contribution Id : ${contributionItem.id}</span> <br/>

    <p><@docContent docItem=contributionDesc /></p>

    <div>
<pre>
<code>
${contributionItem.xml?html}
</code>
</pre>
    </div>

    <br/>
    <br/>

    <b> target ExtensionPoint </b>
    <a href="${Root.path}/${distId}/viewExtensionPoint/${contributionItem.extensionPoint}">
      ${contributionItem.extensionPoint}
    </a>

    <@viewAdditionalDoc docsByCat=contributionDocs.getDocumentationItems(Context.getCoreSession())/>

  </div>

  <#assign nestedLevel=nestedLevel-1/>

</#macro>
