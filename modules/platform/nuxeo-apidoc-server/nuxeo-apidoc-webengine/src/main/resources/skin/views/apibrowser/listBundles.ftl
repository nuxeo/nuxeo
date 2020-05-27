<@extends src="base.ftl">
<@block name="title">All Bundles</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${bundles?size} Bundles</h1>

<div class="tabscontent">

  <table id="contentTable" class="tablesorter">
  <thead>
    <tr>
      <th>
        <@tableFilterArea "bundle"/>
      </th>
      <th>
        Deployment Order
      </th>
    </tr>
  </thead>
  <tbody>
    <#list bundles as bundle>
    <tr>
      <td>
        <div>
          <h4><a title="Bundle Name" href="${Root.path}/${distId}/viewBundle/${bundle.id}" class="itemLink">${bundle.label}</a></h4>
        </div>
      </td>
      <td>
        ${bundle.order}
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
