<#assign style_category = script("getStylerCategory.groovy") />
<#assign preset_groups = script("getPresetGroupsForSelectedCategory.groovy") />
<#assign presets_for_current_group = script("getPresetsForCurrentGroup.groovy") />

<div class="nxthemesToolbox" id="nxthemesStylePicker">

<div class="title">
<img class="close" onclick="javascript:NXThemesStyleEditor.closeStylePicker()"
     src="/nuxeo/site/files/nxthemes-editor/img/close-button.png" width="14" height="14" alt="" />
  Presets - ${style_category}</div>

  <div class="header">PRESETS:
    <div>
      <select name="" id="stylePickerGroupName" onchange="NXThemesStyleEditor.setPresetGroup(this)">
      <#list preset_groups as preset_group>
        <option value="${preset_group}">${preset_group}</option>
      </#list>
      </select>
    </div>
  </div>

  <div class="frame">
    <#list presets_for_current_group as preset_info>
        <div class="selection" onclick="NXThemesStyleEditor.updateFormField('&quot;${preset_info.name}&quot;')">
           ${preset_info.preview}
        </div>
    </#list>
  </div>

</div>

