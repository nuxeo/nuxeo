<@extends src="base.ftl">
<@block name="title">All extension points</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<@filterForm eps?size 'extension point'/>

<#assign showDesc=false>
<#if Context.request.getParameter("showDesc")??>
  <#assign showDesc=true>
</#if>
<#if showDesc>
   <#assign descriptions=This.getDescriptions("NXExtensionPoint")/>
</#if>

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
<script type="text/javascript">
    $(document).ready(function() {
        $("#extensionPointsTable").tablesorter({sortList:[[0,0]], widgets:['zebra']} );
    });
</script>
</@block>

</@extends>
