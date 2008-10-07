<div>

  <#if !styleOfSelectedElement>
      <form action="" class="nxthemesForm"
        onsubmit="NXThemesStyleEditor.createStyle(); return false;">
        <div>
          The element has no style.
          <button>
            Create a style
          </button>
        </div>
      </form>
  
  <#else>

      <style id="previewCss" type="text/css"></style>

      <form class="nxthemesInheritedStyles" onsubmit="return false"
        element="#{nxthemesUiStates.selectedElement.uid}"
        currentThemeName="#{nxthemesUiManager.currentThemeName}">
        <div>
        <label>Inherit style properties from:</label>
        <h:selectOneMenu id="inherited_style" value="#{nxthemesUiManager.inheritedStyleNameOfSelectedElement}"
          rendered="#{not empty nxthemesUiManager.availableNamedStyles}"
          onchange="NXThemesStyleEditor.makeElementUseNamedStyle(this)">
          <f:selectItems value="#{nxthemesUiManager.availableNamedStyles}" />
        </h:selectOneMenu>
        <button onclick="NXThemesStyleEditor.createNamedStyle('#{nxthemesUiStates.selectedElement.uid}', '#{nxthemesUiManager.currentThemeName}')">New style</button>
        <c:if test='#{nxthemesUiManager.inheritedStyleNameOfSelectedElement != ""}'>
          <button onclick="NXThemesStyleEditor.deleteNamedStyle('#{nxthemesUiStates.selectedElement.uid}', '#{nxthemesUiManager.currentThemeName}', '#{nxthemesUiManager.inheritedStyleNameOfSelectedElement}')">Delete '#{nxthemesUiManager.inheritedStyleNameOfSelectedElement}'</button>
        </c:if>
        </div>
      </form>
      
      <div class="nxthemesButtonSelector"
        style="text-align: left; padding: 4px 15px;">
        <span>Layers: </span>
        <ui:repeat value="#{nxthemesUiManager.styleLayersOfSelectedElement}"
          var="layer">
          <span><h:outputText escape="false" value="#{layer.rendered}" />
          </span>
        </ui:repeat>
      </div>

      <table style="width: 100%" cellpadding="10" cellspacing="0">
        <tr>
          <td style="width: 50%; vertical-align: top">
            <fieldset class="nxthemesEditor">
              <legend>
                Preview
              </legend>

              <div id="stylePreviewArea"
                element="#{nxthemesUiStates.selectedElement.uid}">
                <img src="/nuxeo/site/files/nxthemes-editor/img/progressbar.gif" alt=""
                  width="220" height="19"
                  style="padding: 5px; border: 1px solid #ccc; background-color: #fff" />
                <nxthemes:view resource="style-preview.json" />
              </div>
            </fieldset>
            <div id="labelInfo" style="display:none" />

          </td>
          <td style="width: 50%; vertical-align: top">

            <!--  Style properties form -->
            <@nxthemes_panel identifier="style properties"
              url="/nxthemes/editor/styleProperties.ftl"
              controlledBy="style editor perspectives,style editor actions,element form actions"
              visibleInPerspectives="style properties,style picker" />

            <!--  Style picker -->
            <@nxthemes_panel identifier="style picker"
              url="/nxthemes/editor/stylePicker.ftl"
              controlledBy="style editor perspectives,toolbox mover"
              visibleInPerspectives="style picker" />

          </td>
        </tr>
      </table>
    </#if>

</div>

