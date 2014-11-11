<div id="nxthemesThemeSelector">
  <form class="nxthemesThemeSelector" action="javascript:void(0)">
  <label>Theme:</label>
  <select id="theme">
  <#list themes as theme>
    <option <#if theme.name = current_theme_name>selected="selected"</#if>
     value="${theme.path}">${theme.name}</option>
  </#list>
  </select>
  </form>
</div>
