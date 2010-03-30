<#include "/views/component/componentMacros.ftl">
<#macro viewBundle bundleWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign bundleItem=bundleWO.getNxArtifact()/>
  <#assign bundleDocs=bundleWO.getAssociatedDocuments()/>
  <#assign bundleDesc=bundleDocs.getDescription(Context.getCoreSession())/>
  <#assign components=bundleWO.getComponents()/>


  <div id="Bundle.${bundleItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*8}px">
  <A name="Bundle.${bundleItem.id}"> </A>
  <div class="blocTitle bTitle${nestedLevel}" id="Bundle.${bundleItem.id}">  Bundle ${bundleDesc.title} </div>
  <div class="foldablePannel">

  <span class="componentId">Bundle Id : ${bundleItem.id}</span><br/>

  <p><@docContent docItem=bundleDesc /></p>


  <br/>

  <table width="100%">
  <tr>
  <td>
    <table class="bundleInfo">
    <tr> <td> File </td> <td> ${bundleItem.fileName} </td> </tr>
    <tr> <td> Maven id </td> <td> ${bundleItem.artifactId} </td> </tr>
    <tr> <td> Maven group </td> <td> ${bundleItem.artifactGroupId} </td> </tr>
    <tr> <td> Maven version </td> <td> ${bundleItem.artifactVersion} </td> </tr>
    </table>
  </td>
  <td>
    <span class="resourceToggle"> MANIFEST </span>
    <div class="hiddenResource">
    <pre><code>
    ${bundleItem.manifest}
    </code>
    </pre>
    </div>
  </td>
  </tr>
  </table>

  <#if (components?size>0) >

  ${bundleDesc.title} contains ${components?size} components.

  <table class="linkTable">
    <#list components as component>
      <tr>
        <td>
        ${component.associatedDocuments.getDescription(Context.getCoreSession()).title}
        </td>
        <td>
        <A href="#Component.${component.nxArtifact.id}">${component.nxArtifact.id}</A>
        </td>
      </tr>
    </#list>
  </table>

    <#list components as component>
      <@viewComponent componentWO=component />
    </#list>

  </#if>

  </div>
  </div>

  <#assign nestedLevel=nestedLevel-1/>
</#macro>


