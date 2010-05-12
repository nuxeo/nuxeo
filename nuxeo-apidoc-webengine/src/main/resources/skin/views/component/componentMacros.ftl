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
  <div class="blocTitle bTitle${nestedLevel}" id="Component.${componentItem.id}">

  <#if componentItem.xmlPureComponent>
    <img src="${skinPath}/images/${componentDesc.targetType}-xml.png" alt="XML Component"/>
  </#if>
  <#if !componentItem.xmlPureComponent>
    <img src="${skinPath}/images/${componentDesc.targetType}-java.png" alt="Java Component"/>
  </#if>


  ${componentDesc.title}

<#if Root.isEditor()>
  <A href="${Root.path}/${distId}/viewComponent/${componentItem.id}/doc">
  <img src="${skinPath}/images/edit.png" alt="Edit"/>
  </A>
</#if>

<#if This.nxArtifact.id!=componentItem.id>
&nbsp;&nbsp;
    <A href="${Root.path}/${distId}/viewComponent/${componentItem.id}/">
    <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
    </A>
</#if>

  </div>

  <div class="foldablePanel">

  <span class="componentId">Component Id : ${componentItem.id}</span> <br/><br/>

  <span class="builtindoc">
  <#if componentItem.xmlPureComponent>
      ${componentDesc.title} is a pure Xml Component (no java Code).
  </#if>
  <#if !componentItem.xmlPureComponent>
       ${componentDesc.title} is a Java component (implementation class : ${componentItem.componentClass})
  </#if>
  </span>
  <br/>
  <br/>

  <p><@docContent docItem=componentDesc /></p>
  <span class="builtindoc">
  Component XML descriptor
  </span><span class="resourceToggle"> ${componentItem.xmlFileName} </span>
    <div class="hiddenResource">
    <pre><code>
    ${componentItem.xmlFileContent?html}
    </code>
    </pre>
   </div>
  <br/>


  <#if (services?size>0) >
  <br/>
  <b>Declared services</b>
  <p>
  <span class="builtindoc">
  ${componentDesc.title} contains ${services?size} services. <br/>
  </span>

  <#if (services?size>1) >
  <span class="builtindoc">
  Service index : <br/>
  </span>
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
  <br/>
  <b>Declared extension points</b>
  <p>
  <span class="builtindoc">
  ${componentDesc.title} exposes ${xps?size} extension points. <br/>
  </span>

  <#if (xps?size>1) >
  <span class="builtindoc">
  Extension point index : <br/>
  </span>
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
 <br/>
 <b>Declared contributions</b>
  <p>
  <span class="builtindoc">
  ${componentDesc.title} holds ${contribs?size} contributions.<br/>
  </span>

 <#if (contribs?size>1) >
 <span class="builtindoc">
  Contribution index : <br/>
  </span>
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


  <@viewAdditionalDoc docsByCat=componentDocs.getDocumentationItems(Context.getCoreSession())/>
  </div>
  </div>

 <#assign nestedLevel=nestedLevel-1/>
</#macro>


