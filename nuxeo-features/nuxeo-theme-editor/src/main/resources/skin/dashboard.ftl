<#setting url_escaping_charset='UTF-8'>

<@nxthemes_controller resource="dashboard-perspectives.json" />

<div class="nxthemesThemeControlPanelScreen">
<div class="nxthemesThemeControlPanel">

<table style="width: 100%">
<tr>
<td style="width: 19%; vertical-align: top;">

<div class="window">
<div class="title">Theme</div>
<div class="body">
  <ul class="nxthemesSelector">
    <li><a href="javascript:NXThemesEditor.controlPanel()">Overview</a></li>
    <li><a href="javascript:NXThemesEditor.showThemePreview()">Preview theme</a></li>
  </ul>
</div>
</div>

<div class="window">
<div class="title">Basic configuration</div>
<div class="body">

  <ul class="nxthemesSelector">
    <li><a href="javascript:NXThemesEditor.manageSkins()">Skins</a></li>
    <li><a href="javascript:NXThemesEditor.setThemeOptions()">Theme options</a></li>
  </ul>

</div>
</div>

<div class="window">
<div class="title">Advanced configuration</div>
<div class="body">
  <ul class="nxthemesSelector">
    <li><a href="javascript:NXThemesEditor.editCss()">CSS editor</a></li>
    <li><a href="javascript:NXThemesEditor.manageImages()">Image library</a></li>
    <li><a href="javascript:NXThemesEditor.manageThemeBanks()">Theme banks</a></li>
  </ul>
</div>
</div>


<div class="window">
<div class="title">Expert mode</div>
<div class="body">

  <ul class="nxthemesSelector">
    <li><a href="javascript:NXThemesEditor.manageThemes()">Theme manager</a></li>
    <li><a href="javascript:NXThemesEditor.manageStyles()">Style manager</a></li>
    <!-- li><a href="javascript:NXThemesEditor.managePresets()">Preset manager</a></li -->
  </ul>

</div>
</div>

</td>

<td style="width: 1%">
</td>


<td style="width: 80%; vertical-align: top">

    <!-- control panel -->
    <@nxthemes_panel identifier="control panel"
      url="${basePath}/nxthemes-editor/controlPanel"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="control panel" />

    <!-- theme options -->
    <@nxthemes_panel identifier="theme options"
      url="${basePath}/nxthemes-editor/themeOptions"
      controlledBy="dashboard perspectives,color picker"
      visibleInPerspectives="theme options" />

    <!-- css editor -->
    <@nxthemes_panel identifier="css editor"
      url="${basePath}/nxthemes-editor/cssEditor"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="css editor" />

    <!-- skin manager -->
    <@nxthemes_panel identifier="skin manager"
      url="${basePath}/nxthemes-editor/skinManager"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="skin manager" />

    <!-- bank manager -->
    <@nxthemes_panel identifier="bank manager"
      url="${basePath}/nxthemes-editor/bankManager"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="bank manager" />

    <!-- image manager -->
    <@nxthemes_panel identifier="image manager"
      url="${basePath}/nxthemes-editor/imageManager"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="image manager" />

    <!-- preset manager -->
    <@nxthemes_panel identifier="preset manager"
      url="${basePath}/nxthemes-editor/presetManager"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="preset manager" />

    <!-- style manager -->
    <@nxthemes_controller resource="style-manager-actions.json" />
    <@nxthemes_panel identifier="style manager"
      url="${basePath}/nxthemes-editor/styleManager"
      controlledBy="dashboard perspectives,style manager actions"
      visibleInPerspectives="style manager" />

    <!-- theme browser -->
    <@nxthemes_panel identifier="theme browser"
      url="${basePath}/nxthemes-editor/themeBrowser"
      controlledBy="dashboard perspectives"
      visibleInPerspectives="theme browser" />

    <!-- dashboard preview -->
    <@nxthemes_panel identifier="dashboard preview"
      url="${basePath}/nxthemes-editor/dashboardPreview"
      controlledBy="dashboard perspectives"
      filter="activate dashboard preview"
      visibleInPerspectives="dashboard preview" />

</td>
</tr>
</table>

</div>
</div>