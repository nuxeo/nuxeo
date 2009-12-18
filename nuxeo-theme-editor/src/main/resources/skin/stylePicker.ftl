
<div class="nxthemesToolbox" id="nxthemesStylePicker">

<div class="title">
<img class="close" onclick="javascript:NXThemesStyleEditor.closeStylePicker()"
     src="${skinPath}/img/close-button.png" width="14" height="14" alt="" />
  Presets - ${style_category}</div>

  <div class="header">PRESETS:
    <div>
      <select id="stylePickerGroupName" onchange="NXThemesStyleEditor.setPresetGroup(this)">
        <#if !selected_preset_group>
          <option value="" selected="selected">Theme presets (${current_theme_name})</option>
        <#else>
          <option value="">Theme presets (${current_theme_name})</option>
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
      <a class="addPreset" href="javascript:void(0)" onclick="NXThemesEditor.addPreset('${current_theme_name?js_string}', '${style_category?js_string}', 'style picker')">ADD PRESET</a>
    </#if>
    <#list presets_for_selected_group as preset_info>
      <div>
        <#if !selected_preset_group>
          <a class="editPreset" href="javascript:void(0)" onclick="NXThemesEditor.editPreset('${current_theme_name?js_string}', '${preset_info.effectiveName?js_string}', '${preset_info.value?js_string}', 'style picker');">
	      <img src="${basePath}/skin/nxthemes-editor/img/edit-12.png" /></a>
        </#if>
        <#if preset_info.value>
          <div class="selection" title="${preset_info.effectiveName}" onclick="NXThemesStyleEditor.updateFormField('&quot;${preset_info.effectiveName?js_string}&quot;')">
            <div class="name">${preset_info.name}</div>
            <div class="preview">${preset_info.preview}</div>
            <div class="value">${preset_info.value}</div>
          </div>
        <#else>
          <div class="selection" title="${preset_info.effectiveName}" onclick="NXThemesEditor.editPreset('${current_theme_name?js_string}', '${preset_info.effectiveName?js_string}', '${preset_info.value?js_string}', 'style picker');">
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

