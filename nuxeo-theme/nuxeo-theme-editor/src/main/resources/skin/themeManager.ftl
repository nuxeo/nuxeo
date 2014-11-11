<div>

<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>

<div id="nxthemesThemeManager" class="nxthemesScreen">

<h1 class="nxthemesEditor">Themes</h1>

<table cellpadding="0" cellspacing="0" style="width: 100%"><tr>

<td style="vertical-align: top; width: 200px; padding-right: 5px; border-right: 1px dashed #ccc">


<ul class="nxthemesSelector">
<#list themes as theme>
<li <#if theme.name = current_theme_name>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesEditor.selectTheme('${theme.name}', 'theme manager')">
  <#if theme.customized>
    <img src="${skinPath}/img/customized-theme-16.png" width="16" height="16" />
  <#else>
    <#if theme.xmlConfigured>
      <img src="${skinPath}/img/theme-16.png" width="16" height="16" />
    </#if>
    <#if theme.custom>
      <img src="${skinPath}/img/custom-theme-16.png" width="16" height="16" />
    </#if>
  </#if>
  ${theme.name}</a></li>
</#list>
</ul>

</td>
<td style="padding-left: 10px; vertical-align: top;">

<#list themes as theme>
  <#if theme.name = current_theme_name>
      <h2 class="nxthemesEditor">${theme.name}</h2>
      <p>        
        URL: ${theme.src}
        <#if theme.loadingFailed>
          <span class="nxthemesEmphasize">LOADING FAILED</span>
        </#if>
      </p>
      
      <p>
        <#if theme.saveable>
          <button onclick="NXThemesEditor.saveTheme('${theme.src}', 2)">
            <img src="${skinPath}/img/theme-save.png" width="16" height="16" />
            Save
          </button>
        </#if> 

        <#if theme.exportable>  
          <button onclick="window.location='${basePath}/nxthemes-editor/xml_export?theme=${theme.name}&amp;download=1&amp;indent=2'">
            <img src="${skinPath}/img/theme-download.png" width="16" height="16" />
            Download
          </button>
          <button onclick="window.location='${basePath}/nxthemes-editor/xml_export?theme=${theme.name}'">
            <img src="${skinPath}/img/theme-code.png" width="16" height="16" />
            Show source
          </button>
        </#if>
      
        <#if theme.reloadable>
          <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
          <img src="${skinPath}/img/theme-reload.png" width="16" height="16" />
          Restore
          </button>
        </#if>
        <#if theme.loadable>
          <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
          <img src="${skinPath}/img/theme-load.png" width="16" height="16" />          
          Load
          </button>
        </#if>

        <#if theme.repairable>
          <button onclick="NXThemesEditor.repairTheme('${theme.name}')">
            <img src="${skinPath}/img/cleanup-16.png" width="16" height="16" />
            Clean up
          </button>
        </#if>
      </p>
  </#if>
</#list>

</td></tr></table>

</div>

</div>

