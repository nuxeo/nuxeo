<div>

<script type="text/javascript"><!--
window.scrollTo(0,0);
//--></script>

<div id="nxthemesThemeManager" class="nxthemesScreen">

<h1 class="nxthemesEditor">Manage themes</h1>

<table cellpadding="0" cellspacing="0" border="0">
  <tr>
    <th></th>
    <th>theme</th>
    <th>source</th>
    <th style="text-align: center; width: 90px">save theme</th>
    <th style="text-align: center; width: 90px">export theme</th>
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

      <td style="width: 16px">
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
      </td>
      
      <td>     
        <#if theme.exportable>  
          <a href="${basePath}/nxthemes-editor/xml_export?theme=${theme.name}">${theme.name}</a>
        <#else>
          <span class="nxthemesCustomized">${theme.name}</span>
        </#if>
        <#if theme.loadingFailed>
          <span class="nxthemesEmphasize">LOADING FAILED</span>
        <#else>
          &nbsp;
        </#if>
      </td>
      
      <td>
        <#if theme.customized>
          <input class="nxthemesCustomized" value="${theme.src}" style="width: 100%; background: none; border: none" />
        <#else>
          <input value="${theme.src}" style="width: 100%; background: none; border: none" />
        </#if>
      </td>

      <td class="action">
        <#if theme.saveable>
          <button onclick="NXThemesEditor.saveTheme('${theme.src}', 2)">
            <img src="${skinPath}/img/theme-save.png" width="16" height="16" />
            Save
          </button>
        <#else>
          &nbsp;
        </#if>
      </td>
      
      <td class="action">
        <#if theme.exportable>
          <button onclick="window.location='${basePath}/nxthemes-editor/xml_export?theme=${theme.name}&amp;download=1&amp;indent=2'">
            <img src="${skinPath}/img/theme-download.png" width="16" height="16" />
            Download
          </button>
        <#else>
          &nbsp;
        </#if>
      </td>
      
      <td class="action">
          <#if theme.reloadable>
            <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
            <img src="${skinPath}/img/theme-reload.png" width="16" height="16" />
            Reload
            </button>
          <#else>
            &nbsp;
          </#if>
          <#if theme.loadable>
            <button onclick="NXThemesEditor.loadTheme('${theme.src}')">
            <img src="${skinPath}/img/theme-load.png" width="16" height="16" />          
            Load
            </button>
          <#else>
            &nbsp;
          </#if>
      </td>
            
      <td class="action">
        <#if theme.repairable>
          <button onclick="NXThemesEditor.repairTheme('${theme.name}')">
            <img src="${skinPath}/img/theme-repair.png" width="16" height="16" />
            Repair
          </button>
          <#else>
            &nbsp;
          </#if>
      </td>
    </tr>
  </#list>
</table>

</div>

</div>

