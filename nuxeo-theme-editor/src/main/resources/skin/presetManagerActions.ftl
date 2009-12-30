
<#if theme>

<span class="nxthemesButtonHeader">Presets:</span>
    
<@nxthemes_button identifier="show_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('theme presets')"
  label="List by category" />

<@nxthemes_button identifier="application_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('application presets')"
  label="List by palette" />
  
<@nxthemes_button identifier="show_unregistered_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('unregistered presets')"
  label="Find unregistered presets" />


</#if>
