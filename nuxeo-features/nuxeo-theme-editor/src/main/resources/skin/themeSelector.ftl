<div id="nxthemesThemeSelector">
  <form action="javascript:void(0)">
  <label>Theme:</label>
  <select id="theme">
    <#if !themes>
        <option value="">???</option>
    </#if>
    <#list themes as theme>
      <option <#if theme.selected> selected="selected" class="selected"</#if> 
      value="${theme.path}">${theme.name}</option>
    </#list>
    <option value="" style="border-top: 1px solid #999; margin-top: 3px; font-weight: bold">Manage themes ...</option>
  </select>
  </form>
</div>