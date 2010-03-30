<#include "/views/bundle/bundleMacros.ftl">
<#macro viewBundleGroup bundleGroupWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign bundleGroupItem=bundleGroupWO.getNxArtifact()/>
  <#assign bundleGroupDocs=bundleGroupWO.getAssociatedDocuments()/>
  <#assign bundleGroupDesc=bundleGroupDocs.getDescription(Context.getCoreSession())/>
  <#assign bundles=bundleGroupWO.getBundles()/>

  <div id="BundleGroup.${bundleGroupItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*8}px">
  <div class="blocTitle bTitle${nestedLevel}" id="${bundleGroupItem.id}"> BundleGroup ${bundleGroupDesc.title} </div>

  <div class="foldablePannel">
  <p><@docContent docItem=bundleGroupDesc /></p>

  ${bundleGroupDesc.title} is composed of ${bundles?size} bundles.

  <table class="linkTable">
  <#list bundles as bundle>
  <tr>
  <td>
  ${bundle.associatedDocuments.getDescription(Context.getCoreSession()).title}
  </td>
  <td>
  <A href="#Bundle.${bundle.nxArtifact.id}">${bundle.nxArtifact.id}</A>
  </td>
  </tr>
  </#list>
  </table>

  <#list bundles as bundle>
   <@viewBundle bundleWO=bundle />
  </#list>
  </div>
  </div>

  <#assign nestedLevel=nestedLevel-1/>
</#macro>