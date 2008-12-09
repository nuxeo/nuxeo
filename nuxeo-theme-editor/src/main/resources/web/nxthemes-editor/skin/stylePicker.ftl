
<div class="nxthemesToolbox" id="nxthemesStylePicker">

<div class="title">
<img class="close" onclick="javascript:NXThemesStyleEditor.closeStylePicker()"
     src="${skinPath}/img/close-button.png" width="14" height="14" alt="" />
  Presets - ${style_category}</div>

  <div class="header">PRESETS:
    <div>
      <select id="stylePickerGroupName" onchange="NXThemesStyleEditor.setPresetGroup(this)">
        <#if !selected_preset_group>
          <option value="" selected="selected">Custom presets</option>
        <#else>
          <option value="">Custom presets</option>
        </#if>
        <#list preset_groups as preset_group>
          <#if preset_group == selected_preset_group>
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
      <a class="addPreset" href="#" onclick="NXThemesEditor.addPreset('${current_theme_name}', '${style_category}')">ADD PRESET</a>
    </#if>
    <#list presets_for_selected_group as preset_info>
        <div>
        <#if !selected_preset_group>
          <a class="editPreset" href="#" onclick="NXThemesEditor.editPreset('${current_theme_name}', '${preset_info.name}');">
	  <img src="${basePath}/skin/nxthemes-editor/img/edit-12.png" /></a>
        </#if>
        <div class="selection" onclick="NXThemesStyleEditor.updateFormField('&quot;${preset_info.name}&quot;')">
           ${preset_info.preview}
        </div>
	</div>
    </#list>
  </div>

</div>

