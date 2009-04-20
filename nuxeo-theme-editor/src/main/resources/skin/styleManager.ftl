<div>
<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>
<#if selected_named_style>
  <#assign selected_named_style_name = selected_named_style.name>
</#if>

<div id="nxthemesStyleManager">

<div class="nxthemesButtonSelector" style="float: right; padding: 11px 5px 12px 0;">
  <#if style_manager_mode == 'named styles'>            
      <a href="javascript:void(0)" onclick="NXThemesStyleManager.setEditMode('unused styles')">Unused styles</a>
      <a href="javascript:void(0)" class="selected">Named styles</a>
  <#else>
      <a href="javascript:void(0)" class="selected">Unused styles</a>
      <a href="javascript:void(0)" onclick="NXThemesStyleManager.setEditMode('named styles')">Named styles</a>
  </#if>
</div>

<h1 class="nxthemesEditor">Styles</h1>

<table cellpadding="0" cellspacing="0" style="width: 100%"><tr>

<td style="vertical-align: top; width: 200px; padding-right: 5px;">

<ul class="nxthemesSelector">
<#list themes as theme>
<li <#if theme.name = current_theme_name>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesEditor.selectTheme('${theme.name}', 'style manager')">
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

<h2 class="nxthemesEditor" style="text-transform: uppercase">${current_theme_name}</h2>

<#if style_manager_mode = 'named styles'>

<#assign found=false>

<ul class="namedStyleSelector">
<#list named_styles as style>
  <#if selected_named_style & style.uid = selected_named_style.uid>
    <#assign found=true>
  </#if>
  <li><a <#if style.name = selected_named_style_name>class="selected"</#if> href="javascript:NXThemesStyleManager.selectNamedStyle('#{style.uid}')">${style.name}</a></li>
</#list>
</ul>

<#if found>
<form id="nxthemesNamedStyleCSSEditor" class="nxthemesForm" style="padding: 0"
      onsubmit="NXThemesStyleManager.updateNamedStyleCSS(this); return false">
<div>
  <textarea id="namedStyleCssEditor" name="cssSource" rows="15" cols="72"
 style="border: 1px solid #999; width: 100%; height: 250px; font-size: 11px;">${selected_named_style_css}</textarea>
  <input type="hidden" name="style_uid" value="#{selected_named_style.uid}" />
</div>
<div>
  <button type="submit">Save</button>
</div>
</form>
</#if>

<#else>

<p class="nxthemesEditor"><em>These styles are associated with non existing views. They can probably be cleaned up.</em><p>

<#assign styles=themeManager.getStyles(current_theme_name)>
<#list styles as style>

<#assign views=themeManager.getUnusedStyleViews(style)>
<#if views>

<#list views as view>

<form class="unusedViews" action="javascript:void(0)" submit="return false">
  <div>
    <input type="hidden" name="theme_name" value="${current_theme_name}" />
    <input type="hidden" name="style_uid" value="#{style.uid}" />
    <input type="hidden" name="view_name" value="${view}" />
  </div>
   
  <div style="font-size: 11px; font-weight: bold">
    '${view}' view
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

</#if>

</td></tr></table>

</div>

