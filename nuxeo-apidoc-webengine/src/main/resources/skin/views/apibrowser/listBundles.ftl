<@extends src="base.ftl">
<@block name="title">All bundles</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${bundleIds?size} bundles</h1>

<div class="tabscontent">
  <@tableFilterArea "bundle"/>
  <table id="bundlesTable" class="tablesorter">
  <thead>
    <tr>
      <th>
        Bundle
      </th>
    </tr>
  </thead>
  <tbody>
    <#list bundleIds as bundleId>
    <#assign rowCss = (bundleId_index % 2 == 0)?string("even","odd")/>
    <tr class="${rowCss}">
      <td>
        <a href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</a>
      </td>
    </tr>
    </#list>
  </tbody>
  </table>
</div>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#bundlesTable" "[0,0]" />
</@block>

</@extends>
