
<#if style_category>

<div class="nxthemesToolbox" id="nxthemesStylePicker">

<#if resource_bank>
  <#assign resource_bank_name = resource_bank.name />
  <#assign bank_images = resource_bank.getImages() />
</#if>

<div class="title">
<img class="close" onclick="javascript:NXThemesStyleEditor.closeStylePicker()"
     src="${basePath}/skin/nxthemes-editor/img/close-button.png" width="14" height="14" alt="" />
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
    <#list presets_for_selected_group as preset>
      <div>
        <#if !selected_preset_group>
          <a class="editPreset" href="javascript:void(0)" onclick="NXThemesEditor.editPreset('${current_theme_name?js_string}', '${preset.effectiveName?js_string}', '${preset.value?js_string}', 'style picker');">
          <img src="${basePath}/skin/nxthemes-editor/img/edit-12.png" /></a>
        </#if>
        <#assign preset_value=preset.value />
        <#assign category = preset.category />
        <#if preset_value>
          <#assign value=Root.resolveVariables(current_theme_name, resource_bank_name, bank_images, preset_value) />
          <div class="selection" title="${preset.effectiveName}" onclick="NXThemesStyleEditor.updateFormField('&quot;${preset.effectiveName?js_string}&quot;')">
            <div class="name">${preset.name}</div>
            <div class="preview">
              <#if category = 'color'>
                <div style="background-color: ${value}"></div>
              </#if>
              <#if category = 'background'>
                <div style="background: ${value}"></div>
              </#if>
              <#if category = 'font'>
                <div style="font: ${value}; padding-top: 5px">ABC abc</div>
              </#if>
              <#if category = 'image'>
                <div style="background-image: ${value}"></div>
              </#if>
            </div>
            <div class="value">${preset_value?replace(r'${basePath}', '${basePath}')}</div>
          </div>
        <#else>
          <div class="selection" title="${preset.effectiveName}" onclick="NXThemesEditor.editPreset('${current_theme_name?js_string}', '${preset.effectiveName?js_string}', '${preset_value?js_string}', 'style picker');">
            <div class="name">${preset.name}</div>
            <div class="preview"></div>
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

</#if>
