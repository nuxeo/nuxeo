<@extends src="base.ftl">

<@block name="stylesheets">
<style>
  th, td {
  border: 1px solid #D1DBBD;
  vertical-align: top;
  padding: .5em;
  display: table-cell;
  }
  th {
  font-weight: bold;
  text-align: center;
  }
  table {
  border-collapse: collapse;
  border-spacing: 0;
  }
</style>
</@block>
<@block name="header_scripts"></@block>

<@block name="right">
<h1> view Contribution ${contribution.id} </h1>

<h2> View contributed schemas </h2>

<script>
function displaySchema(obj, root) {
  var table = $("<table></table>");
  for (var key in obj) {
    var row = $("<tr></tr>");
    var val = obj[key];
    var th = $("<th></th>");
    th.append(key);

    var td = $("<td></td>");
    if (typeof(val)=="object") {
      displaySchema(val, td);
    } else {
      td.append(val);
    }

    row.append(th);
    row.append(td);
    table.append(row);
  }
  root.append(table);
}
</script>

<#list nxItem.contributionItems as contributionItem>
  <div id="schema_${contributionItem_index}"></div>
</#list>

<script>
jQuery(document).ready(function() {
<#list nxItem.contributionItems as contributionItem>
  var schemaTitle=$("<h3>${renderer.getRenderObjectByIndex(contributionItem_index).name}</h3>");
  $("#schema_${contributionItem_index}").append(schemaTitle);
  displaySchema(${renderer.getRenderObjectByIndex(contributionItem_index).json},$("#schema_${contributionItem_index}"));
</#list>
});
</script>

</@block>

</@extends>