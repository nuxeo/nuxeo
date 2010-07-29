<@extends src="base.ftl">

<@block name="stylesheets">
</@block>


<@block name="header_scripts">
<script>

 var detailIdx=0;

 function fetchDetails(epId) {
    var rowItem = $("span").filter(function() { return this.id=='details_' + epId;})[0];
    var newRowId = 'epDetails_' + epId;
    var existingRowItem = $("div").filter(function() { return this.id==newRowId;});
    if (existingRowItem.length>0) {
       $(existingRowItem[0]).remove();
       return;
    }
    detailIdx+=1;
    var newRow="<div id='" + newRowId ;
    newRow = newRow + "'><span id='epDetailContent" + detailIdx;
    newRow = newRow + "'> loading ep details...</span></div>";
    $(newRow).insertAfter($(rowItem));

    var targetUrl = "/nuxeo/site/distribution/adm/viewExtensionPoint/" + epId + "/simple";

    $.get(targetUrl, function(data) {
      $('#epDetailContent' + detailIdx).html(data);
    });
 }
</script>

</@block>

<@block name="right">

<#include "/docMacros.ftl">


<#list eps as ep>

  <A href="javascript:fetchDetails('${ep.id}')">${ep.label}</A>
  <span id="details_${ep.id}"></span>
  <br/>

</#list>

</@block>

</@extends>