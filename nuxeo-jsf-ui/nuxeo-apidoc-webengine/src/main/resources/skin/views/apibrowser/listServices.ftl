<@extends src="base.ftl">
<@block name="title">All services</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${services?size} services</h1>
<div class="tabscontent">

  <table id="servicesTable" class="tablesorter">
  <thead>
    <tr>
      <th>
        <@tableFilterArea "service"/>
      </th>
    </tr>
  </thead>
  <tbody>
    <#list services as service>
    <tr>
      <td>
        <div>
          <h4><a title="Service Short Name" href="${Root.path}/${distId}/viewService/${service.id}">${service.label}</a></h4>
          <span title="Service Name">${service.id}</span>
       </div>
      </td>
    </tr>
    </#list>
  </tbody>
  </table>
</div>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#servicesTable" "[0,0]" />
</@block>

</@extends>
