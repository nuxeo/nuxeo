<#assign screen="css editor">

<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>

<div class="window">
<div class="title">Edit CSS <#if theme_skin>for skin: ${theme_skin.name}</#if></div>
<div class="body">

<#if current_bank>

<#if theme_skin>
<#assign theme_skin_name = theme_skin.name>

<#if theme_skin.remote && theme_skin.customized>
  <form class="nxthemesForm" style="padding: 4px 0; float: right"
      onsubmit="NXThemesStyleManager.restoreNamedStyle(this, '${screen}'); return false">
    <input type="hidden" name="style_uid" value="#{theme_skin.uid}" />
    <input type="hidden" name="theme_name" value="${current_theme_name}" />
    <div>
    <button type="submit">Restore CSS</button>
    </div>
  </form>
</#if>

  <form id="nxthemesNamedStyleCSSEditor" class="nxthemesForm" style="padding: 0"
      onsubmit="NXThemesStyleManager.updateNamedStyleCSS(this, '${screen}'); return false">
    <input type="hidden" name="style_uid" value="#{theme_skin.uid}" />
    <input type="hidden" name="theme_name" value="${current_theme_name}" />

<#if theme_skin.customized>

  <div>
    <textarea id="namedStyleCssEditor" name="css_source" rows="15" cols="72"
   style="margin-bottom: 10px; border: 1px solid #ccc; font-family: monospace; width: 100%; height: 250px; font-size: 11px;">${theme_skin_css}</textarea>
  </div>
  <div style="float: left">
    <button type="submit">Save</button>
  </div>

<#else>
   <div style="padding: 4px 0; float: right">
    <button type="submit">Customize CSS</button>
   </div>
   <textarea disabled="disabled" id="namedStyleCssEditor" name="css_source" rows="15" cols="72"
   style="margin-bottom: 10px; cursor: default; border: 1px solid #ccc; color: #999; font-family: monospace; width: 100%; height: 250px; font-size: 11px;">
${theme_skin_css}
</textarea>

</#if>
</form>

<div style="clear: both; padding: 5px"></div>

<#else>
  <p>No skin selected.</p>
  <p>
    <a href="javascript:NXThemesEditor.manageSkins()"
       class="nxthemesActionButton">Select a skin</a>
  </p>
</#if>

<#else>
    <p>The <strong>${current_theme.name}</strong> theme is not connected to a bank.</p>
  <p>
    <a href="javascript:NXThemesEditor.manageThemeBanks()"
       class="nxthemesActionButton">Connect to a bank</a>
  </p>
</#if>

</div>
</div>

