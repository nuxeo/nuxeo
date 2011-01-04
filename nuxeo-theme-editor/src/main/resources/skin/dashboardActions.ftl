<#setting url_escaping_charset='UTF-8'>

<#if theme>

<@nxthemes_button identifier="dashboard open canvas editor"
  link="javascript:NXThemesEditor.backToCanvas()"
  icon="${basePath}/skin/nxthemes-editor/img/back-14.png"
  label="Canvas editor" />

<@nxthemes_button identifier="dashboard refresh button"
  link="javascript:NXThemesEditor.loadTheme('${theme.src?js_string}')"
  icon="${basePath}/skin/nxthemes-editor/img/refresh-14.png"
  label="Refresh page" />

<#if theme.customization && theme.saveable>
  <@nxthemes_button identifier="dashboard remove customizations"
  link="javascript:NXThemesEditor.uncustomizeTheme('${theme.src}', 'dashboard actions')"
  icon="${basePath}/skin/nxthemes-editor/img/remove-14.png"
  label="Remove customizations" />
</#if>

<#if theme.customizable>
  <@nxthemes_button identifier="dashboard customize theme"
  link="javascript:NXThemesEditor.customizeTheme('${theme.src}', 'dashboard actions')"
  icon="${basePath}/skin/nxthemes-editor/img/edit-14.png"
  label="Customize theme" />
</#if>

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
    <#if !theme.saveable>
      <li><a href="javascript:NXThemesEditor.loadTheme('${theme.src?js_string}')">Restore original ${theme.name} theme</a></li>
    </#if>
    <#if theme.custom && !theme.customization>
      <li><a href="javascript:NXThemesEditor.deleteTheme('${theme.src?js_string}')">Delete ${theme.name} theme</a></li>
    </#if>
  </ul>
</div>

</#if>