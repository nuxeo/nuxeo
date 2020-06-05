<@extends src="base.ftl">
<@block name="title">All Packages</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<#if Root.isEmbeddedMode()>
  <#assign hideNav=true/>
</#if>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${packages?size} Package<#if packages?size != 1>s</#if></h1>
<div class="tabscontent">
  <table id="contentTable" class="tablesorter">
    <thead>
    <tr>
      <th><@tableFilterArea "package" /></th>
    </tr>
    </thead>
  <tbody>
  <#list packages as pkg>
    <tr>
      <td>
        <div>
          <h4><a title="Package Title" href="${Root.path}/${distId}/viewPackage/${pkg.name}" class="itemLink">${pkg.title?html}</a></h4>
          <div class="itemDetail">
            <span title="Type" class="sticker">${pkg.packageType?html}</span>
            <span title="Package Name">${pkg.name?html}</span>
          </div>
        </div>
      </td>
    </tr>
  </#list>
  </tbody>
  </table>
</div>

</@block>

<@block name="footer_scripts">
  <@tableSortFilterScript "#contentTable" "[0,0]" />
</@block>

</@extends>
