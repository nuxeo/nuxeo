<#assign selected_element_id = script("getSelectedElementId.groovy") />

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

<#if selected_element_id>

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
  url="/nxthemes/editor/elementProperties.ftl"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="edit properties" />

  <@nxthemes_panel
  identifier="element widget"
  url="/nxthemes/editor/elementWidget.ftl"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="assign widget" />

  <@nxthemes_panel
  identifier="element style"
  url="/nxthemes/editor/elementStyle.ftl"
  controlledBy="element editor perspectives,element form actions,style editor actions"
  javascript="/nuxeo/nxthemes-lib/nxthemes-style-editor.js"
  visibleInPerspectives="edit style" />

  <@nxthemes_panel
  identifier="element visibility"
  url="/nxthemes/editor/elementVisibility.ftl"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="set visibility" />

  <@nxthemes_panel
  identifier="element description"
  url="/nxthemes/editor/elementDescription.ftl"
  controlledBy="element editor perspectives,element form actions"
  visibleInPerspectives="set description" />

<#else>
    <p>No element is selected.</p>
</#if>

</div>

</div>

