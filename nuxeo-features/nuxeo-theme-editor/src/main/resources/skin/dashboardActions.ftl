<#setting url_escaping_charset='UTF-8'>

<#if theme>

<@nxthemes_button identifier="dashboard refresh button"
  link="javascript:NXThemesEditor.loadTheme('${theme.src?js_string}')"
  icon="${basePath}/skin/nxthemes-editor/img/refresh-14.png"
  label="Refresh" />

<@nxthemes_button identifier="dashboard more actions"
  classNames="dropList"
  menu="nxthemesDashboardActions"
  label="More actions" />

<div id="nxthemesDashboardActions" style="display: none;">
  <ul class="nxthemesDropDownMenu">
    <#if theme.exportable>
      <#if !theme.saveable>
        <li><a href="javascript:window.location='${basePath}/nxthemes-editor/xml_export?src=${theme.src?url}&amp;download=1&amp;indent=2'">Download theme to your computer</a></li>
      </#if>
      <li><a href="javascript:window.location='${basePath}/nxthemes-editor/xml_export?src=${theme.src?url}'">Show theme source (XML)</a></li>
    </#if>
    <!-- <#if theme.repairable><li><a href="javascript:NXThemesEditor.repairTheme('${theme.src?js_string}')">Repair theme</a></li></#if> -->
    <#if theme.customization>
      <li><a href="javascript:NXThemesEditor.uncustomizeTheme('${theme.src?js_string}')">Restore original ${theme.name} theme</a></li>
    </#if>
    <#if theme.custom && !theme.customization>
      <li><a href="javascript:NXThemesEditor.deleteTheme('${theme.src?js_string}')">Delete ${theme.name} theme</a></li>
    </#if>
  </ul>
</div>

<@nxthemes_button identifier="dashboard open canvas editor"
  link="javascript:NXThemesEditor.backToCanvas()"
  icon="${basePath}/skin/nxthemes-editor/img/canvas-14.png"
  label="Open canvas editor" />

</#if>
