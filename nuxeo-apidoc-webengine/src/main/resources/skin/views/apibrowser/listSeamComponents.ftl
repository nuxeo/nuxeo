<@extends src="base.ftl">
<@block name="title">All Seam components</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.min.js"></script>
</@block>

<@block name="right">

<#if searchFilter??>
  <h1>All Seam components (with filter '${searchFilter}')</h1>
<#else>
  <h1>All Seam components</h1>
</#if>

<table id="seamComponentsTable" class="tablesorter">
  <thead>
  <tr>
    <th>Scope</th>
    <th>Seam component</th>
    <th>Class</th>
  </tr>
  </thead>
<tbody>
<#list seamComponents as component>
<#assign rowCss = (component_index % 2 == 0)?string("even","odd")/>
  <tr class="${rowCss}" style="vertical-align: top">
    <td>${component.scope}</td>
    <td>
      <a href="${Root.path}/${distId}/viewSeamComponent/${component.id}">${component.name}</a>
    </td>
    <td>
      <#assign class=component.className>
      <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(class)}"/>
      <#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${class?replace('.','/')}.html"/>
      <a href="${javaDocUrl}" target="_new">${class}</a>
      <#list component.interfaceNames as iface>
        <#if iface != component.className>
          <br/>
          <#assign class=iface>
          <#assign javaDocBaseUrl="${Root.currentDistribution.javaDocHelper.getBaseUrl(class)}"/>
          <#assign javaDocUrl="${javaDocBaseUrl}/javadoc/${class?replace('.','/')}.html"/>
          <a href="${javaDocUrl}" target="_new">${class}</a>
        </#if>
      </#list>
    </td>
  </tr>
</#list>
</tbody>
</table>

</@block>

<@block name="footer_scripts">
<script type="text/javascript">
    $(document).ready(function() {
        $("#seamComponentsTable").tablesorter({sortList:[[1,0]], widgets:['zebra']} );
    });
</script>
</@block>

</@extends>
