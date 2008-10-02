<#assign viewMode = Context.getCookie("nxthemes.mode", "wysiwyg") />

<div id="nxthemesCanvasModeArea">
<div class="nxthemesButtonSelector">
  <a href="javascript:void(0)" name="wysiwyg" class="<#if viewMode == "wysiwyg">selected</#if>">standard view</a>
  <a href="javascript:void(0)" name="fragment" class="<#if viewMode == "fragment">selected</#if>">fragment view</a>
  <a href="javascript:void(0)" name="layout" class="<#if viewMode == "layout">selected</#if>">layout mode</a>
  <a href="javascript:void(0)" name="area-styles-cell" class="<#if viewMode == "area-styles-cell">selected</#if>">area styles &#187;</a>
  <#if viewMode == "area-styles-page" || viewMode == "area-styles-section" || viewMode == "area-styles-cell"> |
    <a href="javascript:void(0)" name="area-styles-page" class="<#if viewMode == "area-styles-page">selected</#if>">page</a>
    <a href="javascript:void(0)" name="area-styles-section" class="<#if viewMode == "area-styles-section">selected</#if>">sections</a>
    <a href="javascript:void(0)" name="area-styles-cell" class="<#if viewMode == "area-styles-cell">selected</#if>">cells</a>
  </#if>
</div>
</div>
