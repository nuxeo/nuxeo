<#if theme>

<span class="nxthemesButtonHeader">Styles:</span>
    
<@nxthemes_button identifier="show_named_styles"
  controlledBy="theme buttons"
  link="javascript:NXThemesStyleManager.setEditMode('named styles')"
  classNames="selected"
  label="List by name" />
    
<@nxthemes_button identifier="show_unused_styles"
  controlledBy="theme buttons"
  link="javascript:NXThemesStyleManager.setEditMode('unused styles')"
  label="Find unused styles" />  

</#if>