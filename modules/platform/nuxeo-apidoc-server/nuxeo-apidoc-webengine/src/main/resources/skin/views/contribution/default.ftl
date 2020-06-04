<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Contribution ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Contribution <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.component.id}">${nxItem.component.id}</a></div>

<div class="tabscontent">
  <@toc />

  This contribution is part of XML component <a class="tag components" href="${Root.path}/${distId}/viewComponent/${nxItem.component.id}">${nxItem.component.id}</a>
  inside ${nxItem.component.bundle.fileName} ${nxItem.component.xmlFileName}

  <#if nxItem.documentationHtml?has_content>
    <h2>Documentation</h2>
    <div class="documentation">
      ${nxItem.documentationHtml}
    </div>
  </#if>

  <h2>Extension Point</h2>
  Extension point
  <a class="tag extensions" href="${Root.path}/${distId}/viewExtensionPoint/${nxItem.extensionPoint}">${nxItem.extensionPoint?split("--")[1]}</a>
  of component
  <a class="tag components" href="${Root.path}/${distId}/viewComponent/${nxItem.targetComponentName.name}">${nxItem.targetComponentName.name?replace(".*\\.","","r")}</a>.

  <h2>Contributed Items</h2>
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
        </div>
        <#if contributionItem.documentation?has_content>
          <div class="block-description">
            ${contributionItem.documentation}
          </div>
        </#if>
      </li>
    </#list>
    </ul>
    <input id="overrideStart" type="button" value="Generate Override" onclick="toggleOverrideForm(true)" />
    <input id="overrideGen" style="display:none" type="submit" value="Generate XML file" class="button primary" onclick="$.fn.clickButton(this)" />
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

  <h2>XML Source</h2>
  <div>
    <pre><code>${nxItem.xml?html}</code></pre>
  </div>

  <@tocTrigger />

</div>

</@block>
</@extends>
