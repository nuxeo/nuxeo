<@extends src="base.ftl">
<@block name="title">All extension points</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<#if Root.isEmbeddedMode()>
  <#assign hideNav=true/>
</#if>


<@block name="right">
<#include "/docMacros.ftl">

<h1>All extension points (${eps?size})</h1>
<@tableFilterArea/>
<table id="extensionPointsTable" class="tablesorter">
<thead>
  <tr>
    <th>
      Extension point
    </th>
    <th>
      Component (short)
    </th>
    <th>
      Component
    </th>
  </tr>
</thead>
<tbody>
  <#list eps as ep>
  <#assign rowCss = (ep_index % 2 == 0)?string("even","odd")/>
  <tr class="${rowCss}">
    <td>
      <a href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.label}</a>
      <#if showDesc>
        <@inlineEdit ep.id descriptions[ep.id]/>
      </#if>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${ep.simpleId}">${ep.simpleId?replace(".*\\.","","r")}</a>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${ep.simpleId}">${ep.simpleId}</a>
    </td>
  </tr>
  </#list>
<tbody>
</table>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#extensionPointsTable" "[0,0]" />
</@block>

</@extends>
