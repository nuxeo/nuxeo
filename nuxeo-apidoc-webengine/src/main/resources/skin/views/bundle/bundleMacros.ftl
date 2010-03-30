<#macro viewBundle bundleWO >

  <#assign bundleItem=bundleWO.getNxArtifact()/>
  <#assign bundleDocs=bundleWO.getAssociatedDocuments()/>
  <#assign bundleDesc=bundleDocs.getDescription(Context.getCoreSession())/>
  <#assign components=bundleWO.getComponents()/>
i

  <H1> View for ${bundleItem.artifactType} ${bundleItem.id}</H1>

  <h2> ${bundleDesc.title} </h2>

  <p><@docContent docItem=bundleDesc /></p>

  ${bundleDesc.title} is contains ${components?size} components.

</#macro>


