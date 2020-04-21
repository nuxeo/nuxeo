<@extends src="base.ftl">
<@block name="title">All contributions</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${contributions?size} contributions</h1>
<div class="tabscontent">
  <table id="contributionsTable" class="tablesorter">
  <thead>
    <tr>
      <th><@fulltextFilter "contribution" "filterContributions"/></th>
    </tr>
  </thead>
  <tbody>
    <#list contributions as contrib>
    <tr>
      <td>
        <div>
          <h4><a title="Contribution" href="${Root.path}/${distId}/viewContribution/${contrib.id}">${contrib.id}</a></h4>
          <span title="Target Extension Point">${contrib.extensionPoint?split("--")[1]}</span> -
          <span title="Target Component Name">${contrib.targetComponentName.name}</<span>
        </div>
      </td>
    </tr>
    </#list>
  </tbody>
  </table>
</div>

</@block>

<@block name="footer_scripts">
  <@tableSortFilterScript "#contributionsTable" "[0,0]" />
</@block>

</@extends>
