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

  <div id="Component.${componentItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*8}px">

  <A name="Component.${componentItem.id}">  </A>
  <div class="blocTitle bTitle${nestedLevel}" id="Component.${componentItem.id}"> Component ${componentDesc.title}</div>

  <div class="foldablePannel">

  <span class="componentId">Component Id : ${componentItem.id}</span> <br/>

  <p><@docContent docItem=componentDesc /></p>


   <span class="resourceToggle"> XML Definition </span>
    <div class="hiddenResource">
    <pre><code>
    ${componentItem.xmlFileContent?html}
    </code>
    </pre>
   </div>
  <br/>


  <#if (services?size>0) >
  <hr/>
  <p>
  ${componentDesc.title} contains ${services?size} services.

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

    <#list services as service>
      <@viewService serviceWO=service />
    </#list>
  </p>
  </#if>

  <#if (xps?size>0) >
  <hr/>
  <p>
  ${componentDesc.title} exposes ${xps?size} extension points.

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

    <#list xps as xp>
      <@viewExtensionPoint extensionPointWO=xp />
    </#list>
  </p>
  </#if>


 <#if (contribs?size>0) >
 <hr/>
  <p>
  ${componentDesc.title} holds ${contribs?size} contributions.

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

    <#list contribs as contrib>
      <@viewContribution contributionWO=contrib />
    </#list>
  </p>
  </#if>

  </div>
  </div>
 <#assign nestedLevel=nestedLevel-1/>
</#macro>


