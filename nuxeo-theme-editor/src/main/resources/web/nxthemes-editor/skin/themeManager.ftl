<#assign themes=script("getThemeDescriptors.groovy") />
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
    <th>load theme</th>
    <th>save theme</th>
    <th>export to disk</th>
    <th>repair theme</th>
  </tr>
  <#list themes as theme>
    <tr>
      <td>
        <#if theme.exportable>
          <a href="/nuxeo/nxthemes-xml-export?theme=${theme.name}">${theme.name}</a>
        <#else>
          &nbsp;
        </#if>
        <#if theme.loadingFailed>
          <span class="nxthemesEmphasize">LOADING FAILED"</span>
        <#else>
          &nbsp;
        </#if>
      </td>
      <td>${theme.src}</td>
      <td class="action">
        <a href="javascript:void(0)" onclick="NXThemesEditor.loadTheme('${theme.src}')">
          <#if theme.reloadable>
            <img src="/nuxeo/site/files/nxthemes-editor/img/theme-reload.png" width="16" height="16" />
            <span>reload</span>
          <#else>
            &nbsp;
          </#if>
          <#if theme.loadable>
            <img src="/nuxeo/site/files/nxthemes-editor/img/theme-load.png" width="16" height="16" />          
            <span rendered="${theme.loadable}" value="load" />
          <#else>
            &nbsp;
          </#if>
        </a>
      </td>
      <td class="action">
        <#if theme.saveable>
          <a href="javascript:void(0)" onclick="NXThemesEditor.saveTheme('${theme.src}', 2)">
            <img src="/nuxeo/site/files/nxthemes-editor/img/theme-save.png" width="16" height="16" />
            <span>save</span>
          </a>
        <#else>
          &nbsp;
        </#if>
      </td>
      <td class="action">
        <a href="/nuxeo/nxthemes-xml-export?theme=${theme.name}&amp;download=1&amp;indent=2">
          <#if theme.exportable>
            <img src="/nuxeo/site/files/nxthemes-editor/img/theme-download.png" width="16" height="16" />
            <span>download</span>
          <#else>
            &nbsp;
          </#if>
        </a>
      </td>
      <td class="action">
        <a href="javascript:void(0)" onclick="NXThemesEditor.repairTheme('${theme.name}')">
          <img src="/nuxeo/site/files/nxthemes-editor/img/theme-repair.png" width="16" height="16" />
          <span>repair</span>
        </a>
      </td>
    </tr>
  </#list>
</table>

</div>

</div>

