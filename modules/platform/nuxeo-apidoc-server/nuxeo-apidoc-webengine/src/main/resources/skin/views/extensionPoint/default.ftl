<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Extension point ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span></h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a></div>

<div class="tabscontent">
  <div class="subnav">
    <ul>
      <li><a href="#">Documentation</a></li>
      <li><a href="#contribute">Existing Contributions</a></li>
    </ul>
  </div>
  <div class="description">
    ${nxItem.documentationHtml}
  </div>

  <h2>Contribution Descriptor</h2>
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
      $('#highlight-plugin').find('li').show();
      $('#searchMatchResult').html("");

      if (text.trim().length==0) {
        $('#searchMatchResult').html("empty search string!");
        return;
      }

      var elems = $('div.searchableText:contains("' + text +'")');
      if (elems.size()>0) {
        $('div.searchableText').highlight(text);
        $('#searchMatchResult').html(elems.size() + " matching contribution(s)");

        $('#highlight-plugin').find('li').hide();
        elems.each(function(i, elt) {
          console.log(elt);
          console.log($(elt).parent('li'));
          $(elt).parent('li').show();
        });
      } else {
        $('#searchMatchResult').html("no match found");
      }
    }
    </script>

    <ul id="highlight-plugin" class="block-list">
      <#list nxItem.extensions as contrib>
      <li id="${contrib.id}">
        <div class="searchableText">
          <span style="display:none">${contrib.component.bundle.fileName} ${contrib.component.xmlFileName}</span>
          <pre><code>${contrib.xml?xml}</code></pre>
        </div>
        <div class="block-title">
          <a class="components" href="${Root.path}/${distId}/viewComponent/${contrib.component.id}">
          ${contrib.component.bundle.fileName} ${contrib.component.xmlFileName}
          </a>
          &nbsp;
          <a class="override" href="${Root.path}/${distId}/viewComponent/${contrib.component.id}/override/?contributionId=${contrib.id}" target="_blank">
          Override
          </a>
        </div>
      </li>
      </#list>
    </ul>

  <#else>
    <h2>Contributions</h2>
    No known contributions.
  </#if>

</a>

</@block>
</@extends>
