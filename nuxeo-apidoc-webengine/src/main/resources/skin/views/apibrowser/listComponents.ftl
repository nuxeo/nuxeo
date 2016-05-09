<@extends src="base.ftl">
<@block name="title">All components</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${javaComponents?size + xmlComponents?size} components</h1>
<@tableFilterArea "component"/>
<table id="componentsTable" class="tablesorter">
<thead>
  <tr>
    <th>
      Component (short)
    </th>
    <th>
      Component
    </th>
    <th>
      Type
    </th>
  </tr>
</thead>
<tbody>
  <#list javaComponents as component>
  <#assign rowCss = (component_index % 2 == 0)?string("even","odd")/>
  <tr class="${rowCss}">
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${component.id}">${component.label}</a>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${component.id}">${component.id}</a>
    </td>
    <td>
      Java
    </td>
  </tr>
  </#list>
  <#list xmlComponents as component>
  <#assign rowCss = ((javaComponents?size + component_index) % 2 == 0)?string("even","odd")/>
  <tr class="${rowCss}">
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${component.id}">${component.label}</a>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewComponent/${component.id}">${component.id}</a>
    </td>
    <td>
      XML
    </td>
  </tr>
  </#list>
</tbody>
</table>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#componentsTable" "[1,0]" />
</@block>

</@extends>
