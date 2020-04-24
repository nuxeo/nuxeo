<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Contribution ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Contribution <span class="componentTitle">${nxItem.id}</span></h1>

<div class="tabscontent">

  This contribution is part of XML component <a class="tag components" href="${Root.path}/${distId}/viewComponent/${nxItem.component.id}">${nxItem.component.id}</a>
  inside ${nxItem.component.bundle.fileName} ${nxItem.component.xmlFileName}

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
    <form method="POST" action="${Root.path}/${distId}/viewContribution/${nxItem.id}/override">
      <input id="overrideStart" type="button" value="Generate Override" onclick="showOverrideForm()"/>
      <ul class="block-list">
      <#list nxItem.contributionItems as contributionItem>
        <li>
          <div>
            <pre><code>${contributionItem.xml}</code></pre>
          </div>
          <div class="block-title">
            <input id="${contributionItem.id}" type="checkbox" name="${contributionItem.id}" value="${contributionItem.id}" style="display:none"/>
            <label for="${contributionItem.id}">${contributionItem.label}</label>
            <span>${contributionItem.documentation}</span>
          </div>
        </li>
      </#list>
      </ul>
      <input class="button primary" id="overrideGen" type="submit" value="Generate XML file" style="display:none"/>
    </form>
  <script>
  function showOverrideForm(event) {
    $('#overrideStart').css("display", "none");
    $('#overrideGen').css("display", "inline");
    $(':checkbox').css("display","inline");
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
