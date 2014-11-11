<div>

<div id="nxthemesFragmentFactory" class="nxthemesScreen">

<h1 class="nxthemesEditor">Add fragment</h1>

<table class="nxthemesTable">
  <tr>
    <th>Fragment (data source)</th>
    <th>Data type</th>
    <th>Presentation (view)</th>
    <th>Action</th>
  </tr>
<#assign row = 1 />  
<#list fragments as fragment>
  <#list fragment.getViews() as v>
  <#if row % 2 == 1>
    <tr class="odd">
  <#else>
    <tr class="even">
  </#if>
  <#assign row = row + 1/>
    <td>${fragment.getFragmentType().getTypeName()}</td>
    <td>${fragment.getFragmentType().getModelName()}&nbsp;</td>
    <td><img src="${basePath}/nxthemes-editor/render_view_icon?name=${v.getTypeName()}" width="16" height="16" />
    ${v.getViewName()}</td>
    <td><button onclick="NXThemesEditor.addFragment('${fragment.getFragmentType().getTypeName()}/${v.getViewName()}', '${selected_element_id}'); return false;">ADD</button></td>
  </tr>
  </#list>
</#list>
</table>

</div>

</div>