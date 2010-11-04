
<!-- style menu -->
<@nxthemes_view resource="style-menu.json" />   

<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>
<#if selected_named_style>
  <#assign selected_named_style_name = selected_named_style.name>
</#if>


<!-- Style manager -->

<div class="window">
<div class="title">Style manager</div>
<div class="body">

  <table class="nxthemesManageScreen">
  <tr>
    <th style="width: 25%;">Style</th>
    <th style="width: 75%;">CSS properties</th>
  </tr>
  <tr>
  <td>

<ul class="nxthemesSelector">
<#list named_styles as style>
  <li <#if style.name = selected_named_style_name>class="selected"</#if>>
    <a href="javascript:NXThemesStyleManager.selectNamedStyle('#{style.uid}')">
    <img src="${basePath}/skin/nxthemes-editor/img/style-16.png" width="16" height="16"/> ${style.name}</a></li>
</#list>
</ul>


<p class="nxthemesEditor">
  <a class="nxthemesActionButton" href="javascript:NXThemesStyleEditor.createNamedStyle(null, '${theme.name}', 'style manager')">
  Create style</a>
</p>

</td>
<td>

<#if selected_named_style>
<form id="nxthemesNamedStyleCSSEditor" class="nxthemesForm" style="padding: 0"
      onsubmit="NXThemesCssEditor.updateNamedStyleCSS(this); return false">
<div>
  <textarea id="namedStyleCssEditor" name="css_source" rows="15" cols="72"
 style="border: 1px solid #999; width: 100%; height: 250px; font-size: 11px;">${selected_named_style_css}</textarea>
  <input type="hidden" name="style_uid" value="#{selected_named_style.uid}" />
  <input type="hidden" name="theme_name" value="${current_theme_name}" />
</div>
<div style="margin-top: 5px">
  <button type="submit">Save</button>
</div>
</form>

<p class="nxthemesEditor" style="float: right; margin-top: -20px">
   <button class="nxthemesActionButton"
    onclick="NXThemesStyleManager.deleteNamedStyle('${current_theme_name}', '${selected_named_style.name}')">Delete style</button>  
</p>

</#if>
 
</td>
</tr>
</table>

</div>
</div>


<!-- unused styles -->
<!--
<#assign styles=themeManager.getStyles(current_theme_name)>
<#if styles>

<div class="window">
<div class="title">Unused styles</div>
<div class="body">

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

</div>
</div>

</#if>
-->



<!-- page styles -->

<div class="window">
<div class="title">Page styles</div>
<div class="body">

  <form class="nxthemesForm" action="javascript:void(0)"
    onsubmit="NXThemesStyleManager.setPageStyles('${current_theme_name}', this); return false">
  <table class="nxthemesManageScreen">
  <tr>
    <th style="width: 25%;">Page</th>
    <th style="width: 75%;">Style</th>
  </tr>
    
  <#list page_styles?keys as page_name>
  <#assign current_style_name=page_styles[page_name]>
  <tr>
  <td>
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

</div>
</div>


<!-- style dependencies -->

<div class="window">
<div class="title">Style dependencies</div>
<div class="body">

<div style="padding: 10px">
  <#macro listTree (objects)>
  <#if (objects?size > 0)>
    <ul>
      <#list objects as s>
        <li>${s.name}
        
         <ins class="model">
          {"id": "style_${s.name}",
           "type": "named style",
           "data": {
             "title": "${s.name}",
             "id": "${s.name}",
             "theme_name": "${current_theme_name}",
             "styles": [
               <#list named_styles as style>
                 <#if style.name != s.name>
                   {"label": "${style.name}", "choice": "${style.name}", "selected": "false"},
                 </#if>
               </#list>
             ],
             "can inherit": true,
             "inherits": true          
           }
          }
          </ins>
        
          <@listTree Root.listNamedStylesDirectlyInheritingFrom(s) />
        </li>
      </#list>
    </ul>
  </#if>
  </#macro> 
  
  <div class="nxthemesStyleTree">
    <@listTree root_styles/>
  </div>

</div>

</div>
</div>
