<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Contribution ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Contribution <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.component.id}">${nxItem.component.id}</a></div>

<div class="tabscontent">

  This contribution is part of XML component <a class="tag components" href="${Root.path}/${distId}/viewComponent/${nxItem.component.id}">${nxItem.component.id}</a>
  inside ${nxItem.component.bundle.fileName} ${nxItem.component.xmlFileName}

  <h2>Documentation</h2>
  ${nxItem.documentationHtml}
  <@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>
  <#if Root.canAddDocumentation()>
    <div class="tabsbutton">
      <a class="button" href="${This.path}/doc">Manage Documentation</a>
    </div>
  </#if>

  <h2>Extension point</h2>
  Extension point
  <a class="tag extensions" href="${Root.path}/${distId}/viewExtensionPoint/${nxItem.extensionPoint}">${nxItem.extensionPoint?split("--")[1]}</a>
  of component
  <a class="tag components" href="${Root.path}/${distId}/viewComponent/${nxItem.targetComponentName.name}">${nxItem.targetComponentName.name?replace(".*\\.","","r")}</a>.

  <h2>Contributed items</h2>
  <form method="POST" action="${Root.path}/${distId}/viewContribution/${nxItem.id}/override" target="_blank">
    <ul class="block-list">
    <#list nxItem.contributionItems as contributionItem>
      <li>
        <div>
          <pre><code>${contributionItem.xml}</code></pre>
        </div>
        <div class="block-title">
          <input id="${contributionItem.id}" type="checkbox" name="${contributionItem.id}" value="${contributionItem.id}" style="display:none" checked/>
          <label for="${contributionItem.id}">${contributionItem.label}</label>
          <span>${contributionItem.documentation}</span>
        </div>
      </li>
    </#list>
    </ul>
    <input id="overrideStart" type="button" value="Generate Override" onclick="toggleOverrideForm(true)"/>
    <input id="overrideGen" style="display:none" type="submit" value="Generate XML file" class="button primary"/>
    <input id="overrideCancel" style="display:none" type="button" value="Cancel" onclick="toggleOverrideForm(false)" />
  </form>
  <script>
  function toggleOverrideForm(show) {
    $('#overrideStart').css("display", show ? "none" : "inline");
    $('#overrideGen').css("display", show ? "inline" : "none");
    $('#overrideCancel').css("display", show ? "inline" : "none");
    $(':checkbox').css("display", show ? "inline" : "none");
    return false;
  }
  </script>

  <h2>XML source</h2>
  <div>
    <pre><code>${nxItem.xml?html}</code></pre>
  </div>

  <@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</div>

</@block>
</@extends>
