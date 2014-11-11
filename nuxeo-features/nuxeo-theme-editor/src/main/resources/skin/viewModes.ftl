<#assign view_mode = Context.getCookie("nxthemes.mode", "wysiwyg") />

<div id="nxthemesViewModeButtons" class="nxthemesButtonSelector" style="text-align: right; margin: 3px; padding-right: 10px">

  <#if view_mode == "wysiwyg">
    <a href="javascript:NXThemesEditor.setViewMode('fragment')">+ show fragments</a>
  </#if>
  <#if view_mode == "fragment">
    <a href="javascript:NXThemesEditor.setViewMode('wysiwyg')">- hide fragments</a>
  </#if>

  <a href="javascript:<#if view_mode != "wysiwyg" && view_mode != "fragment">NXThemesEditor.setViewMode('wysiwyg')<#else>void(0)</#if>" class="<#if view_mode == "wysiwyg" || view_mode == "fragment">selected</#if>">&#171; normal view</a>

  &nbsp;

  <a href="javascript:NXThemesEditor.setViewMode('layout')" class="<#if view_mode == "layout">selected</#if>">layout</a>

  &nbsp;

  <a href="javascript:NXThemesEditor.setViewMode('area-styles-cell')" class="<#if view_mode == "area-styles-cell" || view_mode == "area-styles-page" || view_mode ==
 "area-styles-section" || view_mode == "area-styles-cell">selected</#if>">area styles &#187;</a>
  <#if view_mode == "area-styles-page" || view_mode == "area-styles-section" || view_mode == "area-styles-cell"> |
    <a href="javascript:NXThemesEditor.setViewMode('area-styles-page')" class="<#if view_mode == "area-styles-page">selected</#if>">page</a>
    <a href="javascript:NXThemesEditor.setViewMode('area-styles-section')" class="<#if view_mode == "area-styles-section">selected</#if>">sections</a>
    <a href="javascript:NXThemesEditor.setViewMode('area-styles-cell')" class="<#if view_mode == "area-styles-cell">selected</#if>">cells</a>
  </#if>
</div>

