<@extends src="base.ftl">
<@block name="title">Extension point ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span></h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a></div>

<div class="tabscontent">
  <@toc />

  <#if nxItem.documentationHtml?has_content>
    <h2>Documentation</h2>
    <div class="documentation">
      ${nxItem.documentationHtml}
    </div>
  </#if>

  <h2>Contribution Descriptors</h2>
  <ul class="descriptors">
    <#list nxItem.descriptors as descriptor>
    <li><@javadoc descriptor true /></li>
    </#list>
  </ul>

  <#if nxItem.extensions?size gt 0>
    <h2>Existing Contributions</h2>
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
          <a class="override button" href="${Root.path}/${distId}/viewComponent/${contrib.component.id}/override/?contributionId=${contrib.id}" target="_blank">
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

  <@tocTrigger />

</@block>

</@extends>
