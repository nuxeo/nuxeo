<div>

<script type="text/javascript"><!--
window.scrollTo(0,0);
//--></script>

<div id="nxthemesThemeManager" class="nxthemesScreen">

<h1 class="nxthemesEditor">Manage themes</h1>

<table cellpadding="0" cellspacing="0" border="0">
  <tr>
    <th>theme</th>
    <th>source</th>
    <th style="text-align: center; width: 90px">save theme</th>
    <th style="text-align: center; width: 90px">export to disk</th>
    <th style="text-align: center; width: 90px">restore theme</th>     
    <th style="text-align: center; width: 90px">repair theme</th>
  </tr>
  <#assign row = 1 /> 
  <#list themes as theme>
  <#if row % 2 == 1>
    <tr class="odd">
  <#else>
    <tr class="even">
  </#if>
  <#assign row = row + 1/>
  
      <td>
        <#if theme.exportable>
          <a href="${basePath}/nxthemes-editor/xml_export?theme=${theme.name}">${theme.name}</a>
        <#else>
          &nbsp;
        </#if>
        <#if theme.loadingFailed>
          <span class="nxthemesEmphasize">LOADING FAILED</span>
        <#else>
          &nbsp;
        </#if>
      </td>
      
      <td>${theme.src}</td>

      <td class="action">
        <#if theme.saveable>
          <button onclick="NXThemesEditor.saveTheme('${theme.src}', 2)">
            <img src="${skinPath}/img/theme-save.png" width="16" height="16" />
            <div>Save</div>
          </button>
        <#else>
          &nbsp;
        </#if>
      </td>
      
      <td class="action">
        <button onclick="window.location='${basePath}/nxthemes-editor/xml_export?theme=${theme.name}&amp;download=1&amp;indent=2'">
          <#if theme.exportable>
            <img src="${skinPath}/img/theme-download.png" width="16" height="16" />
            <div>Download</div>
          <#else>
            &nbsp;
          </#if>
        </button>
      </td>
      
      <td class="action">
          <#if theme.reloadable>
            <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
            <img src="${skinPath}/img/theme-reload.png" width="16" height="16" />
            <div>Reload</div>
            </button>
          <#else>
            &nbsp;
          </#if>
          <#if theme.loadable>
            <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
            <img src="${skinPath}/img/theme-load.png" width="16" height="16" />          
            <div>Load</div>
            </button>
          <#else>
            &nbsp;
          </#if>
      </td>
            
      <td class="action">
        <button onclick="NXThemesEditor.repairTheme('${theme.name}')">
          <img src="${skinPath}/img/theme-repair.png" width="16" height="16" />
          <div>Repair</div>
        </button>
      </td>
    </tr>
  </#list>
</table>

</div>

</div>

