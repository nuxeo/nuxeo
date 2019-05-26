<#include "/docMacros.ftl">
<#include "/views/service/macros.ftl">
<#include "/views/extensionPoint/macros.ftl">
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
    <a name="Component.${componentItem.id}"> </a>
    <div class="blocTitle bTitle${nestedLevel}" id="Component.${componentItem.id}">
      <#if componentItem.xmlPureComponent>
        <img src="${skinPath}/images/${componentDesc.targetType}-xml.png" alt="XML Component"/>
      <#else>
        <img src="${skinPath}/images/${componentDesc.targetType}-java.png" alt="Java Component"/>
      </#if>
      <!-- <span id="${componentDesc.getEditId()}_doctitle">${componentDesc.title}</span> -->
      Component <span class="componentTitle">${componentItem.id}</span>
      <!-- <@quickEditorLinks docItem=componentDesc/> -->
      <#if This.nxArtifact.id!=componentItem.id>
        &nbsp;&nbsp;
        <a href="${Root.path}/${distId}/viewComponent/${componentItem.id}">
          <img src="${skinPath}/images/zoom_in.png" alt="Zoom"/>
        </a>
      </#if>
    </div>

    <div class="foldablePanel">

      <@viewComponentDoc componentItem componentDocs/>
      <@viewComponentImpl componentItem/>
      <@viewComponentServices componentItem/>
      <@viewComponentExtensionPoints componentItem/>
      <@viewComponentContributions componentItem/>

      <h2> XML source </h2>
      <div>
        <pre><code>${componentItem.xmlFileContent?html}</code></pre>
      </div>

      <@viewAdditionalDoc docsByCat=componentDocs.getDocumentationItems(Context.getCoreSession())/>
    </div>
  </div>

  <#assign nestedLevel=nestedLevel-1/>
</#macro>

<#macro viewSecComponents components>
  <h2>Components</h2>
  <#if components?size == 0>
    No components.
  <#else>
    <ul>
      <#list components as component>
      <li><a href="${Root.path}/${distId}/viewComponent/${component.name}">${component.name}</a></li>
      </#list>
    </ul>
  </#if>
</#macro>

<#macro viewComponentDoc componentItem componentDocs>
  <h2> Documentation </h2>
  ${componentItem.documentationHtml}
  <@viewSecDescriptions docsByCat=componentDocs.getDocumentationItems(Context.getCoreSession()) title=false />
</#macro>

<#macro viewComponentImpl componentItem>
  <#if !componentItem.xmlPureComponent>
  <h2> Implementation </h2>
    <#assign componentClass=componentItem.componentClass/>
    <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(componentClass)}"/>
    <ul><li>
      <a href="${javaDocBaseUrl}/javadoc/${componentClass?replace('.','/')}.html" target="_new">${componentClass}</a>
    </li></ul>
  </#if>
</#macro>

<#macro viewComponentServices componentItem>
  <h2> Services </h2>
  <ul>
    <#list componentItem.serviceNames as service>
    <li><a href="${Root.path}/${distId}/viewService/${service}">${service}</a></li>
    </#list>
  </ul>
</#macro>

<#macro viewComponentExtensionPoints componentItem>
  <h2> Extension points </h2>
  <ul>
    <#list componentItem.extensionPoints as ep>
    <li><a href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.name}</a></li>
    </#list>
  </ul>
</#macro>

<#macro viewComponentContributions componentItem>
  <h2> Contributions </h2>
  <ul>
    <#list componentItem.extensions as ex>
    <li><a href="${Root.path}/${distId}/viewContribution/${ex.id?url}">${ex.id}</a></li>
    </#list>
  </ul>
</#macro>
