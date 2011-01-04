<#setting url_escaping_charset='UTF-8'>

<#if theme>

<@nxthemes_button identifier="canvas open dashboard"
  link="javascript:NXThemesEditor.openDashboard()"
  icon="${basePath}/skin/nxthemes-editor/img/dashboard-14.png"
  label="Dashboard" />

<@nxthemes_button identifier="canvas refresh button"
  link="javascript:NXThemesEditor.loadTheme('${theme.src?js_string}')"
  icon="${basePath}/skin/nxthemes-editor/img/refresh-14.png"
  label="Refresh page" />

<#if theme.customizable>
  <@nxthemes_button identifier="canvas customize theme"
  link="javascript:NXThemesEditor.customizeTheme('${theme.src}', 'canvas editor')"
  icon="${basePath}/skin/nxthemes-editor/img/edit-14.png"
  label="Customize theme" />
</#if>

<#if theme.customization && theme.saveable>
    <@nxthemes_button identifier="canvas remove customizations"
  link="javascript:NXThemesEditor.uncustomizeTheme('${theme.src}', 'canvas editor')"
  icon="${basePath}/skin/nxthemes-editor/img/remove-14.png"
  label="Remove customizations" />
</#if>

<@nxthemes_button identifier="canvas theme actions"
  classNames="dropList"
  menu="nxthemesThemeActions"
  label="More actions" />

<div id="nxthemesThemeActions" style="display: none;">
  <ul class="nxthemesDropDownMenu">
    <#if theme.exportable>
      <#if !theme.saveable>
        <li><a href="javascript:window.location='${basePath}/nxthemes-editor/xml_export?src=${theme.src?url}&amp;download=1&amp;indent=2'">Download theme to your computer</a></li>
      </#if>
      <li><a href="javascript:window.location='${basePath}/nxthemes-editor/xml_export?src=${theme.src?url}'">Show theme source (XML)</a></li>
    </#if>
    <!-- <#if theme.repairable><li><a href="javascript:NXThemesEditor.repairTheme('${theme.src?js_string}')">Repair theme</a></li></#if> -->
    <li><a href="javascript:NXThemesEditor.deletePage('${current_page_path?js_string}')">Delete current page (${current_page_name})</a></li>
    <#if !theme.saveable>
      <li><a href="javascript:NXThemesEditor.loadTheme('${theme.src?js_string}')">Restore original ${theme.name} theme</a></li>
    </#if>
    <#if theme.custom && !theme.customization>
      <li><a href="javascript:NXThemesEditor.deleteTheme('${theme.src?js_string}')">Delete ${theme.name} theme</a></li>
    </#if>
  </ul>
</div>

</#if>
