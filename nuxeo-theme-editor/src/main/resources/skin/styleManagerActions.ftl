<#if theme>

<span class="nxthemesButtonHeader">Styles:</span>
    
<@nxthemes_button identifier="named_styles"
  controlledBy="theme buttons"
  link="javascript:NXThemesStyleManager.setEditMode('named styles', 'named_styles')"
  label="Styles by name" />
  
<@nxthemes_button identifier="style_dependencies"
  controlledBy="theme buttons"
  link="javascript:NXThemesStyleManager.setEditMode('style dependencies', 'style_dependencies')"
  label="Style dependencies" />  
    
<@nxthemes_button identifier="page_styles"
  controlledBy="theme buttons"
  link="javascript:NXThemesStyleManager.setEditMode('page styles', 'page_styles')"
  label="Page styles" />  
    
<@nxthemes_button identifier="clean_up"
  controlledBy="theme buttons"
  link="javascript:NXThemesStyleManager.setEditMode('clean up')"
  label="Clean up" />

</#if>