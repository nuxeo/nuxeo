<#include "/views/component/macros.ftl">
<#macro viewBundle bundleWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign bundleItem=bundleWO.getNxArtifact()/>
  <#assign bundleDocs=bundleWO.getAssociatedDocuments()/>
  <#assign bundleDesc=bundleDocs.getDescription(Context.getCoreSession())/>

  <div id="Bundle.${bundleItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
    <a name="Bundle.${bundleItem.id}"> </a>
    <div class="blocTitle bTitle${nestedLevel}" id="Bundle.${bundleItem.id}">
      <img src="${skinPath}/images/${bundleDesc.targetType}.png" alt="Bundle"/>
      Bundle <span class="componentTitle">${bundleItem.id}</span>
      <#if This.nxArtifact.id != bundleItem.id>
        &nbsp;&nbsp;
        <a href="${Root.path}/${distId}/viewBundle/${bundleItem.id}">
          <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
        </a>
      </#if>
    </div>

    <div class="foldablePanel">
      <@viewBundleArtifact bundleItem/>
      <@viewSecDescriptions docsByCat=bundleDocs.getDocumentationItems(Context.getCoreSession()) title=true/>
      <@viewSecManifest bundleItem/>
      <@viewSecComponents bundleItem.components/>
      <@viewAdditionalDoc docsByCat=bundleDocs.getDocumentationItems(Context.getCoreSession())/>
    </div>

  </div>

  <#assign nestedLevel=nestedLevel-1/>
</#macro>

<#macro viewBundleArtifact bundleItem>
  <h2>Maven artifact</h2>
  <table class="listTable">
    <tr><td>file</td><td>${bundleItem.fileName}</td></tr>
    <tr><td>groupId</td><td>${bundleItem.groupId}</td></tr>
    <tr><td>artifactId</td><td>${bundleItem.artifactId}</td></tr>
    <tr><td>version</td><td>${bundleItem.artifactVersion}</td></tr>
  </table>
</#macro>

<#macro viewSecManifest bundleItem>
  <h2> Manifest </h2>
  <div>
    <pre><code>${bundleItem.manifest}</code></pre>
  </div>
</#macro>
