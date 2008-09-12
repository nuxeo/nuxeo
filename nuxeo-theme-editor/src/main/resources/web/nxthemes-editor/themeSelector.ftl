<#assign themes = script("getThemes.groovy") />
<#assign pages = script("getPages.groovy") />

<div id="nxthemesThemeSelectorArea" style="width: 100%">

<table class="nxthemesThemeTabs" cellpadding="0" cellspacing="0"
  border="0">
  <tr>
    <#list themes as theme>
      <td class='ltab ${theme.className}'><img alt="" width="5" height="5"
        src="/nuxeo/site/files/nxthemes-editor/img/l-theme-tab.png" /></td>
      <td class='tab ${theme.className}'><a class="switcher" href="javascript:void(0)"
        name="${theme.link}">${theme.name}</a></td>
      <td class='rtab ${theme.className}'><img alt="" width="5" height="5"
        src="/nuxeo/site/files/nxthemes-editor/img/r-theme-tab.png" /></td>
      <td class="separator"></td>
    </#list>
    <td class="ltab"><img alt="" width="5" height="5" src="/nuxeo/site/files/nxthemes-editor/img/l-theme-tab.png" /></td>
    <td class="tab"><a href="javascript:void(0)" onclick="javascript:NXThemesEditor.addTheme()"> + </a></td>
    <td class="rtab"><img alt="" width="5" height="5" src="/nuxeo/site/files/nxthemes-editor/img/r-theme-tab.png" /></td>
  </tr>
</table>

<div class="nxthemesTabs nxthemesPageTabs">
  <ul>
    <#list pages as page>
      <li class='${page.className}'><a class="switcher" href="javascript:void(0)"
        name="${page.link}">${page.name}</a></li>
    </#list>
    <li><a href="javascript:void(0)" onclick="javascript:NXThemesEditor.addPage('nxthemesUiStates.currentTheme.name')"> + </a></li>
  </ul>

<div style="clear: both"></div>
</div>


</div>
