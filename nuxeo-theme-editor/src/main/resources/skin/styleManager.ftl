

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
  <img src="${basePath}/skin/nxthemes-editor/img/add-14.png" /> Create new style</a>
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
    <img src="${basePath}/skin/nxthemes-editor/img/style-16.png" width="16" height="16"/> ${style.name}</a></li>
</#list>
</ul>

</td>
<td>

<#if selected_named_style>
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
</#if>

</td>
</tr>
</table>

</#if>




<#if style_manager_mode = 'clean up'>

<p class="nxthemesExplanation">Clean up styles by removing those associated with unused widgets.<p>

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
    <img src="${basePath}/skin/nxthemes-editor/img/cleanup-16.png" width="16" height="16" />
    Clean up
  </button>

</form>

</#list>

</#if>
</#list>

</#if>



<#if style_manager_mode = 'page styles'>

  <p class="nxthemesExplanation">Page styles.<p>

  <form class="nxthemesForm" action="javascript:void(0)"
    onsubmit="NXThemesStyleManager.setPageStyles('${current_theme_name}', this); return false">
  <table style="width: 100%;" cellpadding="3" cellspacing="1">
  <tr>
    <th style="text-align: left; width: 25%; background-color: #999; color: #fff">Page</th>
    <th style="text-align: left; width: 75%; background-color: #999; color: #fff">Style</th>
  </tr>
    
  <#list page_styles?keys as page_name>
  <#assign current_style_name=page_styles[page_name]>
  <tr>
  <td style="vertical-align: top">
    ${page_name} 
  </td>
  <td>
    <select name="style_${page_name}">
    <option value=""></option>
    <#list named_styles as style>
      <option value="${style.name}" <#if current_style_name=style.name>selected="selected"</#if>>${style.name}</option>
    </#list>
    </select>
  </td>
  </tr>
  </#list>
  
  <tr>
    <td></td>
    <td><button type="submit">Save</button></td>
  </tr>
  
  </table>
  </form>
  
</#if>



<#if style_manager_mode = 'style dependencies'>

  <p class="nxthemesExplanation">Style dependencies.<p>

  <#macro listTree (objects)>
  <#if (objects?size > 0)>
    <ul>
      <#list objects as s>
        <li>${s.name}
          <@listTree Root.listNamedStylesDirectlyInheritingFrom(s) />
        </li>
      </#list>
    </ul>
  </#if>
  </#macro> 
  
  <div class="nxthemesStyleTree">
    <@listTree root_styles/>
  </div>
  
</#if>


</div>
