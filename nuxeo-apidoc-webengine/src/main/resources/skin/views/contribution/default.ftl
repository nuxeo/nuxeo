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

In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.id?split("--")[0]}">${nxItem.id?split("--")[0]}</a>

<h2>Description</h2>
${nxItem.documentationHtml}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>

<h2>Extension point</h2>
Extension point
<a href="${Root.path}/${distId}/viewExtensionPoint/${nxItem.extensionPoint}">${nxItem.extensionPoint?split("--")[1]}</a>
of component
<a href="${Root.path}/${distId}/viewComponent/${nxItem.targetComponentName.name}">${nxItem.targetComponentName.name?replace(".*\\.","","r")}</a>.

<h2>Contributed items</h2>
<ul>
<#list nxItem.contributionItems as contributionItem>
<li> ${contributionItem.label}
<p> ${contributionItem.documentation} </p>
<span class="resourceToggle">View XML source</span>
<div class="hiddenResource">
  <pre><code>${contributionItem.xml}</code></pre>
</div>
</li>
</#list>
</ul>
<h2>XML source</h2>
<span class="resourceToggle">View XML source</span>
<div class="hiddenResource">
  <pre><code>${nxItem.xml?html}</code></pre>
</div>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
