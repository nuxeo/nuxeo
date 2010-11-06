
<div class="window">
<div class="title">Theme manager</div>
<div class="body">

  <table class="nxthemesManageScreen">
  <tr>
    <th style="width: 50%;">Available themes</th>  
    <th style="width: 50%;">Working list</th>
  </tr>
  <tr>
  <td>
  
    <ul class="nxthemesSelector" style="height: 130px; overflow-x: hidden; overflow-y: scroll; border: 1px solid #ccc">
    <#list available_themes as theme>
    <li><a title="${theme.name}" href="javascript:void(0)"
      onclick="NXThemesEditor.addThemeToWorkspace('${theme.name?js_string}', 'theme browser')">
       <img src="${basePath}/skin/nxthemes-editor/img/theme-16.png" width="16" height="16" />
      <span>${theme.name}</span>
      <span class="info"><img src="${basePath}/skin/nxthemes-editor/img/add-theme-to-list-16.png" width="16" height="16" /> add to list</span></a></li>
    </#list>
    </ul>

  </td>
  <td>



    <ul class="nxthemesSelector" style="height: 130px; overflow-x: hidden; overflow-y: scroll; border: 1px solid #ccc;">
      <#list workspace_themes as theme>
        <#assign selected = (current_theme_name == theme.name) />
        <li>
          <a <#if !selected>onclick="NXThemesEditor.removeThemeFromWorkspace('${theme.name?js_string}', 'theme browser')"</#if>
             href="javascript:void(0)">
            <img src="${basePath}/skin/nxthemes-editor/img/theme-16.png" width="16" height="16" />
            <#if selected>
              <span style="font-style: italic;">${theme.name}</span>
            <#else>
              <span>${theme.name}</span>
              <span class="info"><img src="${basePath}/skin/nxthemes-editor/img/remove-theme-from-list-16.png" width="16" height="16" /> remove from list</span>
            </#if>
          </a> 
        </li>
      </#list>
    </ul>
  </td>
  </tr>
  </table>

<p>
  <a class="nxthemesActionButton" href="javascript:NXThemesEditor.addTheme('theme browser')">
   Create new theme</a>
</p>

</div>
</div>

