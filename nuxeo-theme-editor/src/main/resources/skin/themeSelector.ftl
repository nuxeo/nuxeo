<div id="nxthemesThemeSelector">
  <form action="javascript:void(0)">
  <label>Theme:</label>
  <select id="theme">
    <option disabled="disabled" value="">Select a theme</option>
    <#list themes as theme>
      <option <#if current_theme_name = theme.name> selected="selected" class="selected"</#if>
      value="${theme.path}">${theme.name}</option>
    </#list>
  </select>
  </form>
</div>