<#assign view_mode = Context.getCookie("nxthemes.mode", "wysiwyg") />

<div id="nxthemesCanvasModeArea">
<div class="nxthemesButtonSelector">
  <a href="javascript:void(0)" name="wysiwyg" class="<#if view_mode == "wysiwyg">selected</#if>">standard view</a>
  <a href="javascript:void(0)" name="fragment" class="<#if view_mode == "fragment">selected</#if>">fragment view</a>
  <a href="javascript:void(0)" name="layout" class="<#if view_mode == "layout">selected</#if>">layout mode</a>
  <a href="javascript:void(0)" name="area-styles-cell" class="<#if view_mode == "area-styles-cell">selected</#if>">area styles &#187;</a>
  <#if view_mode == "area-styles-page" || view_mode == "area-styles-section" || view_mode == "area-styles-cell"> |
    <a href="javascript:void(0)" name="area-styles-page" class="<#if view_mode == "area-styles-page">selected</#if>">page</a>
    <a href="javascript:void(0)" name="area-styles-section" class="<#if view_mode == "area-styles-section">selected</#if>">sections</a>
    <a href="javascript:void(0)" name="area-styles-cell" class="<#if view_mode == "area-styles-cell">selected</#if>">cells</a>
  </#if>
</div>
</div>
