<#assign styleCategory = Context.getCookie("nxthemes.editor.styleCategory", "page") />
<#assign presetGroups = script("getPresetGroupsForSelectedCategory.groovy") />
<#assign presetsForCurrentGroup = script("getPresetsForCurrentGroup.groovy") />
<#assign currentPresetGroup = Context.getCookie("nxthemes.editor.presetGroup") />

<div class="nxthemesToolbox" id="nxthemesAreaStyleChooser">

<div class="title">
<img class="close" onclick="javascript:NXThemesEditor.closeAreaStyleChooser()"
     src="/nuxeo/site/files/nxthemes-editor/img/close-button.png" width="14" height="14" alt="" />

Style chooser - ${styleCategory}</div>

<div class="header">PRESETS:
  <div>
    <select id="areaStyleGroupName" onchange="NXThemesEditor.setPresetGroup(this)">
      <#list presetGroups as presetGroup>
        <#if currentPresetGroup == presetGroup>
          <option value="${presetGroup}" selected="selected">${presetGroup}</option>
        <#else>
          <option value="${presetGroup}">${presetGroup}</option>
        </#if>
      </#list>
    </select>
  </div>
</div>

<div class="frame">
  <#if presetsForCurrentGroup>
    <#list presetsForCurrentGroup as presetInfo>
      <div class="selection" onclick="NXThemesEditor.updateAreaStyle('&quot;${presetInfo.name}&quot;')">
          ${presetInfo.preview}
      </div>
    </#list>
  <#else>
    <div class="selection" style="padding: 10px 5px; font-style: italic; font-weight: 1.2em;"
      onclick="NXThemesEditor.updateAreaStyle(null)">
      No style
    </div>
  </#if>
</div>

<div class="footer">
&#32;
</div>

</div>

