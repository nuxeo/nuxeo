<#assign selected_element = script("getSelectedElement.groovy") />

<div>

<script type="text/javascript"><!--
window.scrollTo(0,0);
//--></script>

<div class="nxthemesScreen">

<@nxthemes_controller resource="element-form-actions.json" />
<@nxthemes_controller resource="element-editor-perspectives.json" />
<@nxthemes_controller resource="style-editor-perspectives.json" />
<@nxthemes_controller resource="style-editor-actions.json" />

<h1 class="nxthemesEditor">Element editor</h1>

<#if selected_element>

<@nxthemes_tabs identifier="element editor tabs" styleClass="nxthemesEditTabs">
  <item switchTo="element editor perspectives/edit properties" label="Properties"  />
  <item switchTo="element editor perspectives/assign widget" label="Widget"  />
  <item switchTo="element editor perspectives/edit style" label="Style"  />
  <item switchTo="element editor perspectives/set visibility" label="Visibility"  />
  <item switchTo="element editor perspectives/set description" label="Description"  />    
</@nxthemes_tabs>

<div class="nxthemesEditorFrame">

  <@nxthemes_panel
  identifier="element properties"
  url="/nuxeo/nxthemes/editor/elementProperties.faces"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="edit properties" />

  <@nxthemes_panel
  identifier="element widget"
  url="/nuxeo/nxthemes/editor/elementWidget.faces"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="assign widget" />

  <@nxthemes_panel
  identifier="element style"
  url="/nuxeo/nxthemes/editor/elementStyle.faces"
  controlledBy="element editor perspectives,element form actions,style editor actions"
  javascript="/nuxeo/nxthemes-lib/nxthemes-style-editor.js"
  visibleInPerspectives="edit style" />

  <@nxthemes_panel
  identifier="element visibility"
  url="/nuxeo/nxthemes/editor/elementVisibility.faces"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="set visibility" />

  <@nxthemes_panel
  identifier="element description"
  url="/nuxeo/nxthemes/editor/elementDescription.faces"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="set description" />

<#else>
    <p>No element is selected.</p>
</#if>

</div>

</div>

