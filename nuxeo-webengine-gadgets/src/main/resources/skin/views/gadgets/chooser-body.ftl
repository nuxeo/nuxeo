<style>
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

<table class="gadgetBrowser">
<tr>

 <td>
 <div class="categoryList">
 <ul>
 <#list categories as cat>
  <#if cat==category>
   <li class="category currentCategory">
    <a onclick="selectCategory('${cat_index}','${cat}');" id="cat${cat_index}">
  <#else>
   <li class="category">
    <a onclick="selectCategory('${cat_index}','${cat}');" id="cat${cat_index}">
  </#if>
  ${This.getCategoryLabel(cat)}
  </a></li>
   </#list>
 </ul>
 </div>
 </td>

 <td>
  <div id="gadgetListContainer">
    <#include "/views/gadgets/list.ftl">
  </div>
 </td>
</tr>
</table>
