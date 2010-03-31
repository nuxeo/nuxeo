<#include "/views/service/serviceMacros.ftl">
<#include "/views/extensionPoint/extensionPointMacros.ftl">
<#include "/views/contribution/contributionMacros.ftl">
<#macro viewComponent componentWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign componentItem=componentWO.getNxArtifact()/>
  <#assign componentDocs=componentWO.getAssociatedDocuments()/>
  <#assign componentDesc=componentDocs.getDescription(Context.getCoreSession())/>
  <#assign services=componentWO.getServices()/>
  <#assign xps=componentWO.getExtensionPoints()/>
  <#assign contribs=componentWO.getContributions()/>

  <div id="Component.${componentItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">

  <A name="Component.${componentItem.id}">  </A>
  <div class="blocTitle bTitle${nestedLevel}" id="Component.${componentItem.id}"> Component ${componentDesc.title}

  <A href="${Root.path}/${distId}/viewComponent/${componentItem.id}/doc"> Edit </A>

  </div>

  <div class="foldablePannel">

  <span class="componentId">Component Id : ${componentItem.id}</span> <br/>

  <#if componentItem.xmlPureComponent>
      ${componentDesc.title} is a pure Xml Component (no java Code).
  </#if>
  <#if !componentItem.xmlPureComponent>
       ${componentDesc.title} holds a Java component (implementation class : ${componentItem.componentClass})
  </#if>
  <br/>

  <p><@docContent docItem=componentDesc /></p>

  Component <span class="resourceToggle"> XML Definition </span>.
    <div class="hiddenResource">
    <pre><code>
    ${componentItem.xmlFileContent?html}
    </code>
    </pre>
   </div>
  <br/>


  <#if (services?size>0) >

  <b>Declared services</b>
  <p>
  ${componentDesc.title} contains ${services?size} services. <br/>

  <#if (services?size>1) >
  Service index : <br/>
  <table class="linkTable">
    <#list services as service>
      <tr>
        <td>
        ${service.associatedDocuments.getDescription(Context.getCoreSession()).title}
        </td>
        <td>
        <A href="#Service.${service.nxArtifact.id}">${service.nxArtifact.id}</A>
        </td>
      </tr>
    </#list>
  </table>
  </#if>

    <#list services as service>
      <@viewService serviceWO=service />
    </#list>
  </p>
  </#if>

  <#if (xps?size>0) >
  <b>Declared extension points</b>
  <p>
  ${componentDesc.title} exposes ${xps?size} extension points. <br/>

  <#if (xps?size>1) >
  Extension point index : <br/>
  <table class="linkTable">
    <#list xps as xp>
      <tr>
        <td>
        ${xp.associatedDocuments.getDescription(Context.getCoreSession()).title}
        </td>
        <td>
        <A href="#XP.${xp.nxArtifact.id}">${xp.nxArtifact.name}</A>
        </td>
      </tr>
    </#list>
  </table>
  </#if>

    <#list xps as xp>
      <@viewExtensionPoint extensionPointWO=xp />
    </#list>
  </p>
  </#if>


 <#if (contribs?size>0) >
 <b>Declared contributions</b>
  <p>
  ${componentDesc.title} holds ${contribs?size} contributions.<br/>

 <#if (contribs?size>1) >
  Contribution index : <br/>
  <table class="linkTable">
    <#list contribs as contrib>
      <tr>
        <td>
        ${contrib.associatedDocuments.getDescription(Context.getCoreSession()).title}
        </td>
        <td>
        <A href="#Contrib.${contrib.nxArtifact.id}">${contrib.nxArtifact.id}</A>
        </td>
      </tr>
    </#list>
  </table>
 </#if>

    <#list contribs as contrib>
      <@viewContribution contributionWO=contrib />
    </#list>
  </p>
  </#if>

  </div>
  </div>

 <#assign nestedLevel=nestedLevel-1/>
</#macro>


