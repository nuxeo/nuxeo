<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Extension point ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span></h1>
<div class="include-in">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a></div>

<div class="tabscontent">
  <div class="subnav">
    <ul>
      <li><a href="#">Documentation</a></li>
      <li><a href="#contribute">Existing Contributions</a></li>
    </ul>
  </div>
  <div class="description">
  ${nxItem.documentationHtml}
  <@viewSecDescriptions docsByCat=docs.getDocumentationItems(Context.getCoreSession()) title=false/>
  </div>

  <h2>Descriptors</h2>
  <ul>
    <#list nxItem.descriptors as descriptor>
    <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(descriptor)}"/>
    <li>Javadoc: <a href="${javaDocBaseUrl}/javadoc/${descriptor?replace('.','/')}.html" target="_new">${descriptor}</a>
    </#list>
  </ul>

  <#if nxItem.extensions?size gt 0>
  <h2 id="contribute">Existing Contributions </h2>
  <input type="search" id="searchField" placeholder="Text in contributions"/>
  <input type="button" value="search" onclick="searchContrib($('#searchField').val());"/>
  <span id="searchMatchResult"></span>
  <script>
  function searchContrib(text) {

    $('#highlight-plugin').removeHighlight();
    $('#searchMatchResult').html("");

    if (text.trim().length==0) {
      $('#searchMatchResult').html("empty search string!");
      return;
    }

    var elems = $('div.searchableText:contains("' + text +'")');
    if (elems.size()>0) {
      $('div.searchableText').highlight(text);
      $('#searchMatchResult').html(elems.size() + " matching contribution(s)");
    } else {
      $('#searchMatchResult').html("no match found");
    }
  }
  </script>


    <ul id="highlight-plugin" class="block-list">
      <#list nxItem.extensions as contrib>
      <li>
        <div class="block-title">
          <a href="${Root.path}/${distId}/viewContribution/${contrib.id}">
          ${contrib.component.bundle.fileName} ${contrib.component.xmlFileName}
          </a>
        </div>
        <div class="searchableText">
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

</a>

</@block>
</@extends>
