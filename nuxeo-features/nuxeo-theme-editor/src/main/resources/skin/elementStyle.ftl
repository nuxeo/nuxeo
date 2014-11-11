
<div>

  <#if !style_of_selected_element>

      <form action="" class="nxthemesForm" onsubmit="NXThemesStyleEditor.createStyle(); return false;">
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
        element="#{selected_element.uid}"
        currentThemeName="${current_theme_name}">

        <div>
          <label>Inherit style properties from:</label>
      <select id="inherited_style" id="inherited_style" onchange="NXThemesStyleEditor.makeElementUseNamedStyle(this)">
        <option value=""></option>
        <#list named_styles as style>
          <#if inherited_style_name_of_selected_element == style.name>
            <#assign current_style = style />
            <option value="${style.name}" selected="selected">${style.name}</option>
          <#else>
            <option value="${style.name}">${style.name}</option>
          </#if>
        </#list>
      </select>

          <button onclick="NXThemesStyleEditor.createNamedStyle('#{selected_element.uid}', '${current_theme_name?js_string}', 'element style')">New style</button>
          <#if current_style && !current_style.remote>
            <button onclick="NXThemesStyleEditor.deleteNamedStyle('#{selected_element.uid}', '${current_theme_name?js_string}', '${inherited_style_name_of_selected_element?js_string}')">Delete '${inherited_style_name_of_selected_element}'</button>
          </#if>
        </div>

      </form>

      <div class="nxthemesButtonSelector"
        style="text-align: left; padding: 4px 15px;">
        <span>
          <img style="vertical-align: middle" src="${basePath}/skin/nxthemes-editor/img/layers-16.png" width="16" height="16" />
          Layers: </span>
        <#list style_layers_of_selected_element as layer>
          <span>${layer.rendered}</span>
        </#list>
      </div>

      <table style="width: 100%" cellpadding="10" cellspacing="0">
        <tr>
          <td style="with: 50%; vertical-align: top">
            <fieldset class="nxthemesEditor">
              <legend>
                Preview
              </legend>

              <div id="stylePreviewArea"
                element="#{selected_element.uid}">
                <img src="${basePath}/skin/nxthemes-editor/img/progressbar.gif" alt=""
                  width="220" height="19"
                  style="padding: 5px; border: 1px solid #ccc; background-color: #fff" />
                <@nxthemes_view resource="style-preview.json" />
              </div>
            </fieldset>
            <div id="labelInfo" style="display:none" />

          </td>
          <td style="width: 450px; vertical-align: top">

            <!--  Style properties form -->
            <@nxthemes_panel identifier="style properties"
              url="${basePath}/nxthemes-editor/styleProperties"
              controlledBy="style editor perspectives,style editor actions,element form actions"
              visibleInPerspectives="style properties,style picker" />

          </td>
        </tr>
      </table>
  </#if>

</div>

