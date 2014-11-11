
<#if theme>

<span class="nxthemesButtonHeader">Presets:</span>
    
<@nxthemes_button identifier="theme_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('theme presets', 'theme_presets')"
  label="Theme presets" />

<@nxthemes_button identifier="application_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('application presets', 'application_presets')"
  label="Application presets" />
  
<@nxthemes_button identifier="unregistered_presets"
  controlledBy="theme buttons"
  link="javascript:NXThemesPresetManager.setEditMode('unregistered presets', 'unregistered_presets')"
  label="Unregistered presets" />


</#if>
