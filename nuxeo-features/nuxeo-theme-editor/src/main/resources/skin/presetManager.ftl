
<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>

<div class="window">
<div class="title">Preset manager</div>
<div class="body">

<table class="nxthemesManageScreen">
  <tr>
    <th style="width: 25%;">Category</th>
    <th style="width: 75%;">Presets</th>
  </tr>
  <tr>
    <td>
       <ul class="nxthemesSelector">
         <li <#if selected_preset_category = 'color'>class="selected"</#if>>
              <a href="javascript:NXThemesPresetManager.selectPresetCategory('color')">
              <img src="${basePath}/skin/nxthemes-editor/img/category-16.png" width="16" height="16"/> Color</a></li>
         <li <#if selected_preset_category = 'background'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('background')">
             <img src="${basePath}/skin/nxthemes-editor/img/category-16.png" width="16" height="16"/> Background</a></li>
         <li <#if selected_preset_category = 'font'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('font')">
             <img src="${basePath}/skin/nxthemes-editor/img/category-16.png" width="16" height="16"/> Font</a></li>
         <li <#if selected_preset_category = 'image'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('image')">
             <img src="${basePath}/skin/nxthemes-editor/img/category-16.png" width="16" height="16"/> Image</a></li>
         <li <#if selected_preset_category = 'border'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('border')">
             <img src="${basePath}/skin/nxthemes-editor/img/category-16.png" width="16" height="16"/> Border</a></li>
         <li <#if selected_preset_category = ''>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('')">
             <img src="${basePath}/skin/nxthemes-editor/img/category-16.png" width="16" height="16"/> All categories</a></li>
       </ul>
     </td>
    <td>

<#assign presets = This.getCustomPresets(current_theme_name, selected_preset_category)>

<table style="width: 100%">
<#list presets as preset_info>
<tr>
<td class="preset" style="width: 30%">

<div class="preview" title="${preset_info.value?replace(r'${basePath}', '${basePath}')}">

<#if preset_info.category>
  ${preset_info.preview?replace(r'${basePath}', '${basePath}')}
<#else>
  <div><em style="color: #666"><br/>category not set</em></div>
</#if>

</td>
<td>
  <div>${preset_info.name}</div>
  <div>${preset_info.value}</div>
</td>
</tr>

</#list>
</table>

<#if selected_preset_category>
<p>
  <a class="nxthemesActionButton" href="javascript:NXThemesEditor.addPreset('${current_theme_name?js_string}', '${selected_preset_category?js_string}', 'preset manager')">
  Create new preset</a>
</p>
</#if>

</td>
</tr>
</table>

</div>
</div>


<!-- hardcoded colors -->

<#assign colors=This.getHardcodedColors(current_theme_name)>

<#if colors>

<div class="window">
<div class="title">Hardcoded colors</div>
<div class="body">

<h3 class="nxthemesEditorFocus">These colors could be registered as presets:</h3>

<table class="nxthemesManageScreen">
<#list colors as color>
<tr>
<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.convertValueToPreset('${current_theme_name?js_string}', 'color', '${color?js_string}')" style="background-color: ${color}">&nbsp;</div></div>
  <div class="name">${color}</div>
</td>
</tr>
</#list>
</table>

</div>
</div>

</#if>


<!-- unidentified presets -->

<#assign preset_names=This.getUnidentifiedPresetNames(current_theme_name)>

<#if preset_names>

<div class="window">
<div class="title">Unidentified presets</div>
<div class="body">

<h3 class="nxthemesEditorFocus">These presets need to be defined:</h3>
<table class="nxthemesManageScreen">
<#list preset_names as name>
<tr>
<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.addMissingPreset('${current_theme_name?js_string}', '${name?js_string}')">&nbsp;</div></div>
  <div class="name">${name}</div>
</td>
</tr>
</#list>

</table>

</div>
</div>

</#if>

<!-- hardcoded images -->

<#assign images=This.getHardcodedImages(current_theme_name)>
<#if images>

<div class="window">
<div class="title">Hardcoded images</div>
<div class="body">

<h3 class="nxthemesEditorFocus">These images could be registered as presets ...</h3>

<table class="nxthemesManageScreen">
<#list images as image>
<tr>
<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.convertValueToPreset('${current_theme_name?js_string}', 'image', '${image?js_string}')" style="background:${image}">&nbsp;</div></div>
  <div class="name">${image}</div>
</td>
</tr>
</#list>

</table>

</div>
</div>

</#if>


<div class="window">
<div class="title">Presets by collection</div>
<div class="body">

<table class="nxthemesManageScreen">
  <tr>
    <th style="width: 25%;">Palette</th>
    <th style="width: 75%;">Presets</th>
  </tr>

<tr>
<td style="width: 200px; padding-right: 5px;">

<ul class="nxthemesSelector">
<#list preset_groups as group>
<li <#if group = selected_preset_group>class="selected"</#if>><a href="javascript:NXThemesPresetManager.selectPresetGroup('${group}')">
  <img src="${basePath}/skin/nxthemes-editor/img/palette-16.png" width="16" height="16" />
  ${group}</a></li>
</#list>
</ul>

</td>
<td style="padding-left: 10px;">

<#if selected_preset_group>
<!-- Palettes -->

<table class="nxthemesManageScreen">
<#list This.getGlobalPresets(selected_preset_group) as preset_info>
<tr>
<td class="preset">

<div class="preview" title="${preset_info.value?replace(r'${basePath}', '${basePath}')}">

${preset_info.preview?replace(r'${basePath}', '${basePath}')}</div>
<div class="name">${preset_info.name}</div>

</td>
</tr>

</#list>

</table>
</#if>

</td></tr></table>

</div>
</div>


