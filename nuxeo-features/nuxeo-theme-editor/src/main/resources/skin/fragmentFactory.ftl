<div id="nxthemesFragmentFactory" class="nxthemesScreen">

<h1 class="nxthemesEditor">Add fragment</h1>

<#if selected_element_id>

  <table class="fragmentFactory" cellpadding="0" cellspacing="0">
  <tr>
  <th style="width: 30%">
    1. Select fragment type
  </th>
  <th style="width: 30%">
    2. Select view
  </th>
  <th style="width: 40%">
    3. Add fragment 
  </th>
  </tr>
  
  <tr>
  <td style="vertical-align: top">   
  <ul class="nxthemesSelector">
  <#list fragments as fragment>
    <li <#if fragment.getTypeName() = selected_fragment_type>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesFragmentFactory.selectFragmentType('${fragment.getTypeName()}', 'fragment factory')">
    <img src="${skinPath}/img/fragment-16.png" width="16" height="16" /> ${fragment.getTypeName()}</a></li>
  </#list>
  </ul>
  </td>
  <td style="vertical-align: top">
  <ul class="nxthemesSelector">
  <#list views as view>
    <li <#if view.getViewName() = selected_fragment_view>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesFragmentFactory.selectView('${view.getViewName()}', 'fragment factory')">
    <img src="${skinPath}/img/view-16.png" width="16" height="16" /> ${view.getViewName()}</a></li>
  </#list>
  </ul>
  </td>
  
  <td style="vertical-align: top">
    <#if selected_fragment_type & selected_fragment_view>
    <div>
      <button onclick="NXThemesEditor.addFragment('${selected_fragment_type}/${selected_fragment_view}', '${selected_element_id}'); return false;">ADD</button>
    </div>
    </#if>
  </td>
  </tr>
  </table>
  
<#else>

<p class="nxthemesEditor">
  <em>Cannot find the container to insert a fragment into.</em>
</p>
</#if>

</div>
