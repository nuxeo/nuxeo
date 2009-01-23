<div>

<div id="nxthemesStyleManager" class="nxthemesScreen">

<h1 class="nxthemesEditor">Styles</h1>
<h2 class="nxthemesEditor">Theme: ${current_theme_name}</h2>

<h3 class="nxthemesEditor">Unused style views ...</h3>

<#assign styles=This.getStyles(current_theme_name)>

<#list styles as style>
<#assign views=This.getUnusedStyleViews(style)>

<#if views>

<#list views as view>

<form style="padding: 13px "class="unusedViews" action="javascript:void(0)" submit="return false">
  <div>
    <input type="hidden" name="theme_name" value="${current_theme_name}" />
    <input type="hidden" name="style_uid" value="#{style.uid}" />
    <input type="hidden" name="view_name" value="${view}" />
  </div>
   
  <div style="font-size: 11px; font-weight: bold">
    ${current_theme_name}/ ${view}
  </div>
  
  <pre style="margin: 4px 0; font-size: 10px; background-color: #ffc; border: 1px solid #fc0">${This.renderStyleView(style, view)}</pre>

  <button type="submit">
    <img src="${skinPath}/img/cleanup-16.png" width="16" height="16" />
    Clean up
  </button>

</form>

</#list>

</#if>
</#list>

</div>

