<#include "/views/bundle/macros.ftl">
<#macro viewBundleGroup bundleGroupWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign bundleGroupItem=bundleGroupWO.getNxArtifact()/>
  <#assign bundleGroupDocs=bundleGroupWO.getAssociatedDocuments()/>
  <#assign bundleGroupDesc=bundleGroupDocs.getDescription(Context.getCoreSession())/>
  <#assign bundles=bundleGroupWO.getBundles()/>
  <#assign subGroups=bundleGroupWO.getSubGroups()/>

  <#assign quickEditId=bundleGroupDesc.getEditId()/>

  <div id="BundleGroup.${bundleGroupItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
  <div class="blocTitle bTitle${nestedLevel}" id="${bundleGroupItem.id}"> BundleGroup <span id="${quickEditId}_doctitle" >${bundleGroupDesc.title}</span>

  <@quickEditorLinks docItem=bundleGroupDesc/>

  </div>

  <div class="foldablePanel">
  <p><@docContent docItem=bundleGroupDesc /></p>

  <#if (subGroups?size>0)>
  ${bundleGroupDesc.title} contains ${subGroups?size} sub BundleGroup(s).

  <table class="linkTable">
  <#list subGroups as subGroup>
  <tr>
  <td>
  ${subGroup.associatedDocuments.getDescription(Context.getCoreSession()).title}
  </td>
  <td>
  <A href="#BundleGroup.${subGroup.nxArtifact.id}">${subGroup.nxArtifact.id}</A>
  </td>
  </tr>
  </#list>
  </table>

  <#list subGroups as subGroup>
   <@viewBundleGroup bundleGroupWO=subGroup />
  </#list>
  </#if>

  <span class="builtindoc">
  ${bundleGroupDesc.title} is composed of ${bundles?size} bundle(s).

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
  </span>
  <@viewAdditionalDoc docsByCat=bundleGroupDocs.getDocumentationItems(Context.getCoreSession())/>
  </div>
  </div>

  <#assign nestedLevel=nestedLevel-1/>
</#macro>
