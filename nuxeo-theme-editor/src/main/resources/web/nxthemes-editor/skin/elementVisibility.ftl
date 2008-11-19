
<div>

<fieldset class="nxthemesEditor"><legend>Element visibility</legend>

<form id="nxthemesElementVisibility" class="nxthemesForm" action="" onsubmit="return false">

<div>
  <input type="hidden" name="id" value="#{selected_element.uid}" />
</div>

<p>
  <label>Always visible</label>
  <#if is_selected_element_always_visible>
    <input type="checkbox" id="alwaysVisible" name="alwaysVisible" checked="checked" />
  <#else>
    <input type="checkbox" id="alwaysVisible" name="alwaysVisible" />
  </#if>
</p>

<#if !is_selected_element_always_visible>
<p>
  <label>Visible in perspectives</label>
  <select id="perspectives" name="perspectives" multiple="multiple">
    <#list perspectives as perspective>
      <#if perspectives_of_selected_element?seq_contains(perspective.name)>
        <option value="${perspective.name}" selected="selected">${perspective.title}</option>
      <#else>
        <option value="${perspective.name}">${perspective.title}</option>
      </#if>
    </#list>
  </select>
</p>
</#if>

  <button type="submit">Update</button>
</form>

</fieldset>
</div>

