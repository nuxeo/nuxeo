
<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>

<#assign saveable=current_theme && current_theme.saveable>

<div class="window">
<div class="title">Theme options</div>
<div class="body">

<form class="nxthemesForm"
      onsubmit="NXThemesThemeOptions.updatePresets(this); return false">

    <input type="hidden" name="theme_name" value="${current_theme.name}" />

<#assign categories = ["color", "background", "font", "image"] />
<#assign hasField=false />
<#list categories as category>
<#assign presets = This.getCustomPresets(current_theme.name, category)>

<#if presets>
<#assign hasField=true />
<#list presets as preset_info>
  <p>
    <label style="padding: 2px; text-align: right">${preset_info.name} <#if preset_info.label>(${preset_info.label})</#if>&nbsp;</label>
    <input type="text"
        <#if category = 'color'>class="color" style="border-color: #333"</#if>
        id="nxthemes_preset_${preset_info.name}"
        name="preset_${preset_info.name}"
        value="${preset_info.value}" />
    <#if category = 'background' | category = 'image'>
      <a class="nxthemesActionButton"
         href="javascript:void(0)" onclick="NXThemesEditor.selectEditField('nxthemes_preset_${preset_info.name}', 'image manager')">Browse</a>
    </#if>
    <span class="description">${preset_info.description}</span>
  </p>
</#list>
</#if>
</#list>

<#if hasField>
  <p>
    <button <#if !saveable>disabled="disabled"</#if> type="submit" >Save</button>
  </p>
</#if>
</form>

<#if !hasField>
  <p>No theme options available for this theme</p>
</#if>

</div>
</div>


<#if !saveable>
  <div id="nxthemesTopBanner" style="position: absolute">
    <div class="nxthemesInfoMessage">
      <img src="${basePath}/skin/nxthemes-editor/img/error.png" width="16" height="16" style="vertical-align: bottom" />
      <span>Before you can modify theme options you need to customize the <strong>${current_theme.name}</strong> theme.</span>
      <button class="nxthemesActionButton"
       onclick="NXThemesEditor.customizeTheme('${current_theme.src}', 'theme options')">Customize theme</button>
    </div>
  </div>       
</#if>

