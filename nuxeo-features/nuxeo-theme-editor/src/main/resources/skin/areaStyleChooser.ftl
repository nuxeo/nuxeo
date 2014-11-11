
<div class="nxthemesToolbox" id="nxthemesAreaStyleChooser">

<div class="title">
<img class="close" onclick="javascript:NXThemesEditor.closeAreaStyleChooser()"
     src="${skinPath}/img/close-button.png" width="14" height="14" alt="" />
Style chooser - ${style_category}</div>

<div class="header">PRESETS:
  <div>
    <select id="areaStyleGroupName" onchange="NXThemesEditor.setPresetGroup(this)">
      <#if !selected_preset_group>
        <option value="" selected="selected">Theme presets (${current_theme_name})</option>
      <#else>
        <option value="">Theme presets (${current_theme_name})</option>
      </#if>
      <#list preset_groups as preset_group>
        <#if selected_preset_group == preset_group>
          <option class="locked" value="${preset_group}" selected="selected">${preset_group}</option>
        <#else>
          <option class="locked" value="${preset_group}">${preset_group}</option>
        </#if>
      </#list>
    </select>
  </div>
</div>

<div class="frame">
  <#if !selected_preset_group>
    <a class="addPreset" href="javascript:void(0)" onclick="NXThemesEditor.addPreset('${current_theme_name}', '${style_category}', 'area style chooser')">ADD PRESET</a>
  </#if>
  <div class="selection"
    onclick="NXThemesEditor.updateAreaStyle(null)">
    No style
    <div class="noStyle"></div>
  </div>
  <#list presets_for_selected_group as preset_info>
    <div>    
      <#if !selected_preset_group>
        <a class="editPreset" href="javascript:void(0)" onclick="NXThemesEditor.editPreset('${current_theme_name}', '${preset_info.effectiveName}', '${preset_info.value}', 'area style chooser');">
	    <img src="${basePath}/skin/nxthemes-editor/img/edit-12.png" /></a>
      </#if>   
      <#if preset_info.value> 
        <div class="selection" title="${preset_info.effectiveName}" onclick="NXThemesEditor.updateAreaStyle('&quot;${preset_info.effectiveName}&quot;')">
          <div class="name">${preset_info.name}</div>
          <div class="preview">${preset_info.preview}</div>
          <div class="value">${preset_info.value}</div>
        </div>
      <#else>
        <div class="selection" title="${preset_info.effectiveName}" onclick="NXThemesEditor.editPreset('${current_theme_name}', '${preset_info.effectiveName}', '${preset_info.value}', 'area style chooser');">
          <div class="name">${preset_info.name}</div>
          <div class="preview">${preset_info.preview}</div>
          <div class="value">???</div>
        </div> 
      </#if>
    </div>        
  </#list>
</div>

<div class="footer">
&nbsp;
</div>

</div>

