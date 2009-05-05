<div>
<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>

<div id="nxthemesThemeManager" class="nxthemesScreen">

<h1 class="nxthemesEditor">Themes</h1>

<a onclick="NXThemesEditor.editCanvas()" class="nxthemesBack">Back to canvas</a>

<table cellpadding="0" cellspacing="0" style="width: 100%"><tr>
<td style="vertical-align: top; width: 200px; padding-right: 5px;">

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

<form style="float: right" action="javascript:void(0)">
  <div>
    <button onclick="javascript:NXThemesEditor.addTheme()">
    <img src="${skinPath}/img/add-theme-16.png" width="16" height="16" />
    Add theme</button>
  </div>
</form>

<#list themes as theme>
  <#if theme.name = current_theme_name>
      <h2 class="nxthemesEditor" style="text-transform: uppercase">${theme.name}</h2>
      <p class="nxthemesEditor">        
        SOURCE: <strong>${theme.src}</strong>
        <#if theme.lastLoaded && theme.loadingFailed>
          <span class="nxthemesEmphasize">LOADING FAILED</span>
        </#if>
      </p>
      
      <#assign template_engines=theme.templateEngines>
      <#if template_engines>
      <p class="nxthemesEditor">
        COMPATIBILITY:
        <#list template_engines as template_engine>
          <strong>${template_engine}</strong>
        </#list>
      </p>
      </#if>
      
      <#if theme.customized>
        <p class="nxthemesEditor"><em>This theme is currently being hidden by another theme with the same name. </em></p>
      
      <#else>
      
        <#if theme.custom>
          <p class="nxthemesEditor"><em>This theme can be saved by clicking on the SAVE button.</em></p>
        </#if>
      
        <#if !theme.saveable>
          <p class="nxthemesEditor"><em>This theme cannot be saved directly, use the DOWNLOAD button instead.</em></p>
        </#if>

      <p class="nxthemesEditor">
        <button <#if !theme.saveable>disabled="disabled"</#if> onclick="NXThemesEditor.saveTheme('${theme.src}', 2)">
          <img src="${skinPath}/img/theme-save.png" width="16" height="16" />
          Save
        </button>
        
        <#if theme.exportable>  
          <button onclick="window.location='${basePath}/nxthemes-editor/xml_export?src=${theme.src}&amp;download=1&amp;indent=2'">
            <img src="${skinPath}/img/theme-download.png" width="16" height="16" />
            Download
          </button>
          <button onclick="window.location='${basePath}/nxthemes-editor/xml_export?src=${theme.src}'">
            <img src="${skinPath}/img/theme-code.png" width="16" height="16" />
            Show XML
          </button>
        </#if>
      
        <#if theme.repairable>
          <button onclick="NXThemesEditor.repairTheme('${theme.src}')">
            <img src="${skinPath}/img/cleanup-16.png" width="16" height="16" />
            Clean up
          </button>
        </#if>
        
        <#if theme.reloadable>
          <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
          <img src="${skinPath}/img/theme-reload.png" width="16" height="16" />
          Restore from file
          </button>
        </#if>
        <#if theme.loadable>
          <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
          <img src="${skinPath}/img/theme-load.png" width="16" height="16" />          
          Load from file
          </button>
        </#if>
        
        <button <#if !theme.custom>disabled="disabled"</#if> onclick="NXThemesEditor.deleteTheme('${theme.src}')">
          <img src="${skinPath}/img/theme-delete.png" width="16" height="16" />          
          Delete theme
        </button>
      </p>
    
    </#if>
  </#if>
</#list>

</td></tr></table>

</div>

</div>

