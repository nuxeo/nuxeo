<@extends src="base.ftl">
<@block name="title">All contributions</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>All contributions (${contributions?size})</h1>
<@tableFilterArea/>
<table id="contributionsTable" class="tablesorter">
<thead>
  <tr>
    <th>
      Contribution
    </th>
    <th>
      Target extension point
    </th>
    <th>
      Target component
    </th>
  </tr>
</thead>
<tbody>
  <#list contributions as contrib>
  <#assign rowCss = (contrib_index % 2 == 0)?string("even","odd")/>
  <tr class="${rowCss}">
    <td>
      <a href="${Root.path}/${distId}/viewContribution/${contrib.id}">${contrib.id}</a>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewExtensionPoint/${contrib.extensionPoint}">${contrib.extensionPoint?split("--")[1]}</a>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${contrib.targetComponentName.name}">${contrib.targetComponentName.name}</a>
    </td>
  </tr>
  </#list>
</tbody>
</table>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#contributionsTable" "[1,0]" />
</@block>

</@extends>
