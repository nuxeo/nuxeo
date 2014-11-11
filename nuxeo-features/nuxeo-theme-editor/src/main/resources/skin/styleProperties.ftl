
<div>

  <fieldset class="nxthemesEditor">
    <legend>
      Properties
    </legend>

    <div class="nxthemesButtonSelector"
      style="float: right; margin-top: -40px">
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
          <p style="margin-bottom: 10px;">
            <label>
              Selector
            </label>
	        <select id="viewName" onchange="NXThemesStyleEditor.chooseStyleSelector(this)">
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

              <div class="nxthemesButtonSelector" style="padding: 3px; text-align: right"">
                <span>categories: </span>
                <#list style_categories as category>
                  ${category.rendered}
                </#list>
              </div>
       
     
            <#if element_style_properties>
              <div style="height: 220px; margin-top: 5px; margin-bottom: 15px; overflow-y: scroll; overflow-x: hidden">
                <#list element_style_properties as property>
                  <p>${property.rendered}</p>
                </#list>
              </div>
              <button type="submit">
                Save
              </button>
            </#if>
        </form>
      
      <!-- Inline CSS editing -->
      <#else>
        <form id="nxthemesElementStyleCSS" class="nxthemesForm" action=""
          onsubmit="return false">
          <div>
            <textarea id="csseditor" name="cssSource" rows="15" cols="72"
              style="width: 100%; height: 250px; font-size: 11px;">${rendered_style_properties}</textarea>
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

