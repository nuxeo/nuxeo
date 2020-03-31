<@extends src="base.ftl">
<@block name="title">All Seam components</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<#if Root.isEmbeddedMode()>
  <#assign hideNav=true/>
</#if>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${seamComponents?size} Seam components</h1>

<div class="tabscontent">
  <@tableFilterArea "seam component"/>
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
    <tr class="${rowCss}">
      <td><span class="sticker">${component.scope}</span></td>
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
</div>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#seamComponentsTable" "[1,0]" />
</@block>

</@extends>
