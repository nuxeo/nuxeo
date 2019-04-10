<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Contribution ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Contribution <span class="componentTitle">${nxItem.id}</span>
  <a href="${Root.path}/${distId}/viewComponent/${nxItem.id?split("--")[0]}" title="go to parent component">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

This contribution is part of XML component <a href="${Root.path}/${distId}/viewComponent/${nxItem.component.id}">${nxItem.component.id}</a>
inside ${nxItem.component.bundle.fileName} ${nxItem.component.xmlFileName}

<h2>Description</h2>
${nxItem.documentationHtml}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>

<h2>Extension point</h2>
Extension point
<a href="${Root.path}/${distId}/viewExtensionPoint/${nxItem.extensionPoint}">${nxItem.extensionPoint?split("--")[1]}</a>
of component
<a href="${Root.path}/${distId}/viewComponent/${nxItem.targetComponentName.name}">${nxItem.targetComponentName.name?replace(".*\\.","","r")}</a>.

<h2>Contributed items</h2>
<form method="POST" action="${Root.path}/${distId}/viewContribution/${nxItem.id}/override">
<ul>
<#list nxItem.contributionItems as contributionItem>
<li>
<input type="checkbox" name="${contributionItem.id}" value="${contributionItem.id}" style="display:none"/>
 ${contributionItem.label}
<p> ${contributionItem.documentation} </p>
<span class="resourceToggle">View XML source</span>
<div class="hiddenResource">
  <pre><code>${contributionItem.xml}</code></pre>
</div>
</li>
</#list>
</ul>
<input id="overrideStart" type="button" value="Generate Override" onclick="showOverrideForm()"/>
<input id="overrideGen" type="submit" value="Generate XML file" style="display:none"/>
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
<span class="resourceToggle">View XML source</span>
<div class="hiddenResource">
  <pre><code>${nxItem.xml?html}</code></pre>
</div>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
