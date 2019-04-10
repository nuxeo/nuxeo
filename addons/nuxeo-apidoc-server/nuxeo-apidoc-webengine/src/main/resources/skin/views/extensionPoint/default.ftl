<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Extension point ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span>
  <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}" title="Go to parent component">
    <img src="${skinPath}/images/up.gif"/>
  </a>
</h1>

In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a>

<h2>Documentation</h2>
${nxItem.documentationHtml}
<@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>

<h2>Descriptors</h2>
<ul>
  <#list nxItem.descriptors as descriptor>
  <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(descriptor)}"/>
  <li>Javadoc: <a href="${javaDocBaseUrl}/javadoc/${descriptor?replace('.','/')}.html" target="_new">${descriptor}</a>
  </#list>
</ul>

<#if nxItem.extensions?size gt 0>
<h2>Contributions (${nxItem.extensions?size}) </h2>
<input type="text" id="searchField"/>
<input type="button" value="search text in contributions" onclick="searchContrib($('#searchField').val());"/>
<span id="searchMatchResult"></span>
<script>
function searchContrib(text) {
  $('#highlight-plugin').removeHighlight();
  $('div.searchableText').addClass('hiddenResource');
  $('#searchMatchResult').html("");
  var elems = $('div.searchableText:contains("' + text +'")');
  for (var i = 0; i < elems.size(); i++) {
    var elem = $(elems[i]);
    elem.removeClass('hiddenResource');
  }
  if (elems.size()>0) {
    $('div.searchableText').highlight(text);
    $('#searchMatchResult').html(elems.size() + " matching contribution(s)");
  } else {
    $('#searchMatchResult').html("no match found");
  }
}
</script>


  <ul>
    <#list nxItem.extensions as contrib>
    <li>
      <a href="${Root.path}/${distId}/viewContribution/${contrib.id}">
      ${contrib.component.bundle.fileName} ${contrib.component.xmlFileName}
      </A>
      <span class="resourceToggle">View XML source</span>
<div class="hiddenResource searchableText">
  <span style="display:none">${contrib.component.bundle.fileName} ${contrib.component.xmlFileName}</span>
  <pre><code>${contrib.xml?xml}</code></pre>
</div>
    </li>
    </#list>
  </ul>
<#else>
<h2>Contributions</h2>
No known contributions.
</#if>

<@viewAdditionalDoc docsByCat=docs.getDocumentationItems(Context.getCoreSession())/>

</@block>
</@extends>
