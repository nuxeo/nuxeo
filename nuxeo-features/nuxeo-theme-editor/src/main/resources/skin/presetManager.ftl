
<!-- preset menu -->
<@nxthemes_view resource="preset-menu.json" />   

<div id="nxthemesPresetManager" class="nxthemesPresets nxthemesScreen">

<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>
  

<#if preset_manager_mode = 'theme presets'>

<h1 class="nxthemesEditor">Manage presets</h1>

<#if selected_preset_category>
<div style="float: right">
  <a class="nxthemesActionButton" href="javascript:NXThemesEditor.addPreset('${current_theme_name?js_string}', '${selected_preset_category?js_string}', 'preset manager')">
  <img src="${skinPath}/img/add-14.png" /> Create new preset</a>
</div>
</#if>

<p class="nxthemesExplanation">List presets by category</p>

<table style="width: 100%;" cellpadding="3" cellspacing="1">
  <tr>
    <th style="text-align: left; width: 25%; background-color: #999; color: #fff">Category</th>
    <th style="text-align: left; width: 75%; background-color: #999; color: #fff">Presets</th>
  </tr>
  <tr>
    <td style="vertical-align: top">
       <ul class="nxthemesSelector">
         <li <#if selected_preset_category = 'color'>class="selected"</#if>>
              <a href="javascript:NXThemesPresetManager.selectPresetCategory('color')">
              <img src="${skinPath}/img/category-16.png" width="16" height="16"/> Color</a></li>
         <li <#if selected_preset_category = 'background'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('background')">
             <img src="${skinPath}/img/category-16.png" width="16" height="16"/> Background</a></li>
         <li <#if selected_preset_category = 'font'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('font')">
             <img src="${skinPath}/img/category-16.png" width="16" height="16"/> Font</a></li>
         <li <#if selected_preset_category = 'image'>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('image')">
             <img src="${skinPath}/img/category-16.png" width="16" height="16"/> Image</a></li>
 <li <#if selected_preset_category = ''>class="selected"</#if>>
             <a  href="javascript:NXThemesPresetManager.selectPresetCategory('')">
             <img src="${skinPath}/img/category-16.png" width="16" height="16"/> <em style="color: #666">Unsorted</em></a></li>             
       </ul>
     </td>
    <td>
    

<#assign presets = This.getCustomPresets(current_theme_name, selected_preset_category)>


<table cellspacing="0" cellpadding="1" style="width: 100%">
<#assign count = 0 />
<#assign row = 1 /> 

<#list presets as preset_info>
<#assign row = (count % 10) +1 /> 

  <#if row == 0>
    <tr>
  </#if>
<td class="preset">

<div class="preview" title="${preset_info.value}">
<ins class="model">
  {"id": "preset_${current_theme_name}_${preset_info.name}",
   "type": "preset",
   "data": {
     "id": "${preset_info.id}",
     "theme_name": "${current_theme_name}",
     "name": "${preset_info.name}",
     "value": "${preset_info.value}",
     "categories": [
       {"label": "Color", "choice": "color"
        <#if preset_info.category = 'color'>, "selected": "true"</#if>},
       {"label": "Background", "choice": "background"
        <#if preset_info.category = 'background'>, "selected": "true"</#if>},
       {"label": "Font", "choice": "font"
        <#if preset_info.category = 'font'>, "selected": "true"</#if>},
       {"label": "Image", "choice": "image"      
        <#if preset_info.category = 'image'>, "selected": "true"</#if>}
     ],
     "editable": true,
     "copyable": true,
     "pastable": true,
     "deletable": true
     }
  }
</ins>

<#if preset_info.category>
${preset_info.preview}
<#else>
<div><em style="color: #666"><br/>category not set</em></div>
</#if>
</div>
<div class="name">${preset_info.name}</div>
</td>

  <#if row == 10>
    </tr>
  </#if>
  
  <#assign count = count + 1/>
</#list>

<#if row < 10>
  <#list row..9 as i>
     <#if i == row>
       <td id="paste_${current_theme_name}_${count}">
         &nbsp;
         <ins class="model">
         {"id": "paste_${current_theme_name}_${count}",
          "type": "preset",
          "data": {
            "id": "",
            "theme_name": "${current_theme_name}",
            "name": "",
            "value": "",
            "editable": false,
            "copyable": false,
            "pastable": true,
            "deletable": false
          }
         }
         </ins>
       </td>
     <#else>
       <td></td>
     </#if>
     
  </#list>
  </tr>
</#if>

</table>


<#assign preset_names=This.getUnidentifiedPresetNames(current_theme_name)>

<#if preset_names>
<h3 class="nxthemesEditorFocus">These presets need to be defined:</h3>
<table cellspacing="0" cellpadding="1" style="width: 100%">
<#assign count = 0 />
<#assign row = 1 /> 

<#list preset_names as name>
<#assign row = (count % 10) +1 /> 

  <#if row == 0>
    <tr>
  </#if>

<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.addMissingPreset('${current_theme_name?js_string}', '${name?js_string}')">&nbsp;</div></div>
  <div class="name">${name}</div>
</td>

  <#if row == 10>
    </tr>
  </#if>
  
  <#assign count = count + 1/>
  </#list>

<#if row < 10>
  <#list row..9 as i>
       <td></td>
  </#list>
  </tr>
</#if>

</table>

</#if>

</#if>

<#if preset_manager_mode = 'unregistered presets'>

<h1 class="nxthemesEditor">Manage presets</h1>
<p class="nxthemesExplanation">Find unregistered presets (colors, images, ...)</p>

<#assign colors=This.getHardcodedColors(current_theme_name)>

<#if colors>
<h3 class="nxthemesEditorFocus">These colors could be registered as presets:</h3>

<table cellspacing="5" cellpadding="4" style="width: 100%">
<#assign count = 0 />
<#assign row = 1 />
<#assign row = (count % 10) +1 /> 

  <#if row == 0>
    <tr>
  </#if>
<#list colors as color>
<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.convertValueToPreset('${current_theme_name?js_string}', 'color', '${color?js_string}')" style="background-color: ${color}">&nbsp;</div></div>
  <div class="name">${color}</div>
</td>

  <#if row == 10>
    </tr>
  </#if>
  
  <#assign count = count + 1/>
</#list>

<#if row < 10>
  <#list row..9 as i>
      <td></td>
  </#list>
  </tr>
</#if>
        
</table>

</#if>



<#assign images=This.getHardcodedImages(current_theme_name)>

<#if images>
<h3 class="nxthemesEditorFocus">These images could be registered as presets ...</h3>

<table cellspacing="5" cellpadding="4" style="width: 100%">
<#assign count = 0 />
<#assign row = 1 />
<#assign row = (count % 10) +1 /> 

  <#if row == 0>
    <tr>
  </#if>
<#list images as image>
<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.convertValueToPreset('${current_theme_name?js_string}', 'image', '${image?js_string}')" style="background:${image}">&nbsp;</div></div>
  <div class="name">${image}</div>
</td>

  <#if row == 10>
    </tr>
  </#if>
  
  <#assign count = count + 1/>
</#list>

<#if row < 10>
  <#list row..9 as i>
      <td></td>
  </#list>
  </tr>
</#if>
        
</table>

</#if>

</tr>
</td>
</table>

</#if>

</div>

</div>

