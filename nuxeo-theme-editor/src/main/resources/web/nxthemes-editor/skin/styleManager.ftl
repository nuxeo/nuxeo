<div>

<#assign themeManager=This.getThemeManager()>

<div id="nxthemesStyleManager">

<h1 class="nxthemesEditor">Styles</h1>

<table cellpadding="0" cellspacing="0" style="width: 100%"><tr>

<td style="vertical-align: top; width: 200px; padding-right: 5px; border-right: 1px dashed #ccc">

<h2 class="nxthemesEditor">Themes</h2>
<ul class="nxthemesSelector">
<#list themeManager.getThemeNames() as theme_name>
<li <#if theme_name = current_theme_name>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesEditor.selectTheme('${theme_name}', 'style manager')">
  <img src="${skinPath}/img/theme-16.png" width="16" height="16" />
  ${theme_name}</a></li>
</#list>
</ul>


</td>
<td style="padding-left: 10px; vertical-align: top;">

<h2 class="nxthemesEditor">Theme: ${current_theme_name}</h2>
<h3 class="nxthemesEditor">Named styles</h3>
<ul>
<#list named_styles as style>
  <li>${style.name}</li>
</#list>
</ul>

<#assign styles=themeManager.getStyles(current_theme_name)>
<#list styles as style>

<#assign views=themeManager.getUnusedStyleViews(style)>
<#if views>

<h3 class="nxthemesEditorFocus">Unused view style ...</h3>

<#list views as view>

<form style="padding: 10px 8px 16px 8px" class="unusedViews" action="javascript:void(0)" submit="return false">
  <div>
    <input type="hidden" name="theme_name" value="${current_theme_name}" />
    <input type="hidden" name="style_uid" value="#{style.uid}" />
    <input type="hidden" name="view_name" value="${view}" />
  </div>
   
  <div style="font-size: 11px; font-weight: bold">
    ${view}
  </div>
  
  <pre style="margin: 4px 0 6px 0; font-size: 10px; background-color: #ffc; border: 1px solid #fc0">${This.renderStyleView(style, view)}</pre>

  <button type="submit">
    <img src="${skinPath}/img/cleanup-16.png" width="16" height="16" />
    Clean up
  </button>

</form>

</#list>

</#if>
</#list>

</td></tr></table>

</div>

