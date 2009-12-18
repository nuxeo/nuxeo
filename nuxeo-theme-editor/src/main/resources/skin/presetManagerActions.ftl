
<#if theme>

<span class="nxthemesButtonHeader">Presets:</span>
    
<@nxthemes_button identifier="show_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('theme presets')"
  classNames="selected"
  label="List presets by category" />

<@nxthemes_button identifier="show_unregistered_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('unregistered presets')"
  label="Find unregistered presets" />

</#if>
