<div id="nxthemesStyleManager" class="nxthemesScreen">

<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>
<#if selected_named_style>
  <#assign selected_named_style_name = selected_named_style.name>
</#if>

<h1 class="nxthemesEditor">Manage styles</h1>


<#if style_manager_mode = 'named styles'>


<div style="float: right">
  <a class="nxthemesActionButton" href="javascript:NXThemesStyleEditor.createNamedStyle(null, '${theme.name}', 'style manager')">
  <img src="${skinPath}/img/add-14.png" /> Create new style</a>
</div>

  <p class="nxthemesExplanation">List styles by name.<p>

  <table style="width: 100%;" cellpadding="3" cellspacing="1">
  <tr>
    <th style="text-align: left; width: 25%; background-color: #999; color: #fff">Style</th>
    <th style="text-align: left; width: 75%; background-color: #999; color: #fff">CSS properties</th>
  </tr>
  <tr>
  <td style="vertical-align: top">

<ul class="nxthemesSelector">
<#list named_styles as style>
  <li <#if style.name = selected_named_style_name>class="selected"</#if>>
    <a href="javascript:NXThemesStyleManager.selectNamedStyle('#{style.uid}')">
    <img src="${skinPath}/img/style-16.png" width="16" height="16"/> ${style.name}</a></li>
</#list>
</ul>

</td>
<td>

<form id="nxthemesNamedStyleCSSEditor" class="nxthemesForm" style="padding: 0"
      onsubmit="NXThemesStyleManager.updateNamedStyleCSS(this); return false">
<div>
  <textarea id="namedStyleCssEditor" name="css_source" rows="15" cols="72"
 style="border: 1px solid #999; width: 100%; height: 250px; font-size: 11px;">${selected_named_style_css}</textarea>
  <input type="hidden" name="style_uid" value="#{selected_named_style.uid}" />
  <input type="hidden" name="theme_name" value="${current_theme_name}" />
</div>
<div>
  <button type="submit">Save</button>
</div>
</form>

</td>
</tr>
</table>

</#if>




<#if style_manager_mode = 'unused styles'>

<p class="nxthemesExplanation">Find unused styles.<p>

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
</div>
