<@extends src="base.ftl">
<@block name="title">All services</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.min.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<@filterForm services?size 'service'/>

<table id="servicesTable" class="tablesorter">
<thead>
  <tr>
    <th>
      Service (short)
    </th>
    <th>
      Service
    </th>
  </tr>
</thead>
<tbody>
  <#list services as service>
  <#assign rowCss = (service_index % 2 == 0)?string("even","odd")/>
  <tr class="${rowCss}">
    <td>
      <a href="${Root.path}/${distId}/viewService/${service.id}">${service.label}</a>
    </td>
    <td>
      <a href="${Root.path}/${distId}/viewService/${service.id}">${service.id}</a>
    </td>
  </tr>
  </#list>
</tbody>
</table>

</@block>

<@block name="footer_scripts">
<script type="text/javascript">
    $(document).ready(function() {
        $("#servicesTable").tablesorter({sortList:[[0,0]], widgets:['zebra']} );
    });
</script>
</@block>

</@extends>
