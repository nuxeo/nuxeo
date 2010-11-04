<#setting url_escaping_charset='UTF-8'>

<#if theme>

<@nxthemes_button identifier="dashboard open canvas editor"
  link="javascript:NXThemesEditor.backToCanvas()"
  icon="${basePath}/skin/nxthemes-editor/img/back-14.png"
  label="Canvas editor" />

<#if theme.custom>
    <@nxthemes_button identifier="dashboard remove customizations"
  link="javascript:NXThemesEditor.uncustomizeTheme('${theme.src}', 'dashboard actions')"
  icon="${basePath}/skin/nxthemes-editor/img/remove-14.png"  
  label="Remove customizations" />
</#if>

<#if !theme.saveable>
  <@nxthemes_button identifier="dashboard customize theme"
  link="javascript:NXThemesEditor.customizeTheme('${theme.src}', 'dashboard actions')"
  icon="${basePath}/skin/nxthemes-editor/img/edit-14.png"  
  label="Customize theme" />
</#if>

</#if>