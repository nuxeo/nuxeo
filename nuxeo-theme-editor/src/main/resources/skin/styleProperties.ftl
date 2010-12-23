
<div>

  <fieldset class="nxthemesEditor">
    <legend>
      Properties
    </legend>

    <div class="nxthemesButtonSelector"
      style="float: right; margin-top: -40px; margin-right: -10px">
      <span>Edit mode:</span>
      <#if style_edit_mode == 'form'>
          <a href="javascript:void(0)" onclick="NXThemesStyleEditor.setStyleEditMode('css', 'form')">CSS</a>
          <a href="javascript:void(0)" class="selected">form</a>
      <#else>
          <a href="javascript:void(0)" class="selected">CSS</a>
          <a href="javascript:void(0)" onclick="NXThemesStyleEditor.setStyleEditMode('form', 'css')">form</a>
      </#if>
    </div>

      <!-- Edit form -->
      <#if style_edit_mode == 'form'>
        <form id="nxthemesElementStyle" class="nxthemesForm" action="" onsubmit="return false">
          <p style="margin-bottom: 5px;">
            <label style="width: 154px">
              Selector
            </label>
            <select style="width: 247px" id="viewName" onchange="NXThemesStyleEditor.chooseStyleSelector(this)">
              <#list style_selectors as selector>
                <#if selector == selected_style_selector>
                  <option value="${selector}" selected="selected">${selector}</option>
                <#else>
                  <option value="${selector}">${selector}</option>
                </#if>
              </#list>
            </select>
            <input type="hidden" name="id" value="#{selected_element.uid}" />
            <input type="hidden" name="path" value="${selected_style_selector}" />
            <input type="hidden" name="viewName" value="${selected_view_name}" />
          </p>

          <div class="nxthemesCssInspectorActions" style="width: 395px; text-align: right">
            <a href="javascript:void(0)"
               onclick="NXThemesStyleEditor.expandAllCategories()">(+) Expand all</a>
            <a href="javascript:void(0)"
               onclick="NXThemesStyleEditor.collapseAllCategories()">(-) Collapse all</a>
          </div>

          <div class="nxthemesCssInspector">
            <table>
            <#if element_style_properties>
              <#list element_style_properties as property>
              <tr>
                <td class="label"><label for="${property.id}">${property.label}</label></td>
                <td class="input">${property.inputWidget}</td>
              </tr>
              </#list>
            </#if>

            <#if all_style_properties>
              <#list all_style_properties?keys as category>
                <#assign visible=selected_css_categories?seq_contains(category) />
                <tr>
                  <td colspan="2" class="nxthemesCategoryHeader">
                    <a href="javascript:void(0)" onclick="NXThemesStyleEditor.toggleCssCategory(this, '${category?js_string}')">
                    <#if visible>
                      <span class="nxthemesStyleCategoryClose">&nbsp;</span>
                    <#else>
                      <span class="nxthemesStyleCategoryOpen">&nbsp;</span>
                    </#if>
                    ${category}</a>
                  </td>
                </tr>
                <#list all_style_properties[category] as property>
                  <#if !property.value>
                    <tr class="nxthemesStyleField" category="${category}" <#if !visible>style="display: none"</#if>>
                      <td class="label"><label for="${property.id}">${property.label}</label></td>
                      <td class="input">${property.inputWidget}</td>
                    </tr>
                  </#if>
                </#list>
              </#list>
            </#if>
            </table>
          </div>

          <button type="submit">
            Save
          </button>
        </form>

      <!-- Inline CSS editing -->
      <#else>
        <form id="nxthemesElementStyleCSS" class="nxthemesForm" action=""
          onsubmit="return false">
          <div>
            <textarea id="csseditor" name="cssSource" rows="15" cols="72"
              style="width: 100%; height: 250px; font-family: monospace; font-size: 11px;">${rendered_style_properties}</textarea>
            <input type="hidden" name="id" value="#{selected_element.uid}" />
            <input type="hidden" name="viewName" value="${selected_view_name}" />
          </div>
          <div style="padding-top: 10px">
            <button type="submit">Save</button>
          </div>
        </form>
      </#if>
  </fieldset>

</div>

