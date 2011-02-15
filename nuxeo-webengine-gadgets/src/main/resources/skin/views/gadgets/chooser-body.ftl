<style>
.category { background-color:#EEEEEE;margin:2px;padding:2px;border:1px #AAAAAA solid;cursor:pointer}
.currentCategory { background-color:#CCCCCC;}
.gadget { margin: 2px; border:1px #AAAAAA solid;cursor:pointer;padding:2px}
.currentGadget {background-color:#CCCCCC;}
.gadgetTitle {}
<#if mode=="popup">
.addButton {float:right;}
<#else>
.addButton {float:right;display:none}
</#if>
</style>

<script>
var galleryBaseUrl = '${This.path}';
</script>
<script type="text/javascript" src="${skinPath}/scripts/gadget-gallery.js"></script>

<table>
<tr>

 <td style="vertical-align:top">
 <div style="overflow:auto;">
 <#list categories as cat>
  <#if cat==category>
  <div class="category currentCategory" onclick="selectCategory('${cat_index}','${cat}');" id="cat${cat_index}">
  <#else>
  <div class="category" onclick="selectCategory('${cat_index}','${cat}');" id="cat${cat_index}">
  </#if>
  ${This.getCategoryLabel(cat)}
  </div>
 </#list>
 </div>
 </td>

 <td width="300px" style="vertical-align:top">
  <div style="overflow:auto;height:350px;text-align:left;" id="gadgetListContainer">
    <#include "/views/gadgets/list.ftl">
  </div>
 </td>

 <td width="320px" style="vertical-align:top">
   <div id="gadgetDetails" style="overflow:auto;height:350px">
   </div>
 </td>
</tr>
</table>
