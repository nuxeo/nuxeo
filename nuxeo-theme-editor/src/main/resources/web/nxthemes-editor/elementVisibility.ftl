<#assign selected_element_id = script("getSelectedElementId.groovy") />
<#assign is_selected_element_always_visible = script("isSelectedElementAlwaysVisible.groovy") />
<#assign perspectives = script("getPerspectives.groovy") />

<div>

<fieldset class="nxthemesEditor"><legend>Element visibility</legend>

<form id="nxthemesElementVisibility" class="nxthemesForm" action="" onsubmit="return false">

<div>
  <input type="hidden" name="id" value="${selected_element_id}" />
</div>

<p>
  <label>Always visible</label>
  <input type="checkbox" id="alwaysVisible" value="${is_selected_element_always_visible}" />
</p>

<#if is_selected_element_always_visible>
<p>
  <label>Visible in perspectives</label>
  <select id="perspectives" name="">
    <#list perspectives as perspective>
      <option value="${perspective}">${perspective}</option>
    </#list>
  </select>
</p>
</#if>

  <button type="submit">Update</button>
</form>

</fieldset>
</div>

