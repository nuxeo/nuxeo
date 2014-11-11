<div>
<#assign themeManager=This.getThemeManager()>
<#assign themes=themeManager.getThemeDescriptors()>

<!-- preset menu -->
<@nxthemes_view resource="preset-menu.json" />     
      
<div id="nxthemesPresetManager">

<div class="nxthemesButtonSelector" style="float: right; padding: 11px 5px 12px 0;">
  <#if preset_manager_mode == 'by theme'>            
      <a href="javascript:void(0)" onclick="NXThemesPresetManager.setEditMode('by palette')">By palette</a>
      <a href="javascript:void(0)" class="selected">By theme</a>
  <#else>
      <a href="javascript:void(0)" class="selected">By palette</a>
      <a href="javascript:void(0)" onclick="NXThemesPresetManager.setEditMode('by theme')">By theme</a>
  </#if>
</div>

<h1 class="nxthemesEditor">Manage presets</h1>

<a onclick="NXThemesEditor.editCanvas()" class="nxthemesBack">Back to canvas</a>

<table cellpadding="0" cellspacing="0" style="width: 100%"><tr>
<td style="vertical-align: top; width: 200px; padding-right: 5px;">

<#if preset_manager_mode == 'by theme'>

<h3 class="nxthemesEditor">THEMES</h3>
<ul class="nxthemesSelector">
<#list themes as theme>
<li <#if theme.name = current_theme_name>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesEditor.selectTheme('${theme.name}', 'preset manager')">
  <#if theme.customized>
    <img src="${skinPath}/img/customized-theme-16.png" width="16" height="16" />
  <#else>
    <#if theme.xmlConfigured>
      <img src="${skinPath}/img/theme-16.png" width="16" height="16" />
    </#if>
    <#if theme.custom>
      <img src="${skinPath}/img/custom-theme-16.png" width="16" height="16" />
    </#if>
  </#if>
  ${theme.name} <span style="font-size: 11px; font-style: italic; overflow: hidden">(${theme.src})</span></a></li>
</#list>
</ul>

<#else>

<h3 class="nxthemesEditor">PALETTES</h3>
<ul class="nxthemesSelector">
<#list preset_groups as group>
<li <#if group = selected_preset_group>class="selected"</#if>><a href="javascript:void(0)" 
  onclick="NXThemesPresetManager.selectPresetGroup('${group}')">
  <img src="${skinPath}/img/palette-16.png" width="16" height="16" />
  ${group}</a></li>
</#list>
</ul>
</#if>

</td>
<td style="padding-left: 10px; vertical-align: top;">

<#if preset_manager_mode == 'by theme'>

<h2 class="nxthemesEditor" style="text-transform: uppercase">${current_theme_name}</h2>

<#assign presets = This.getCustomPresets(current_theme_name)>

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
<div class="category">${preset_info.category}</div>
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
  <div class="preview"><div onclick="NXThemesPresetManager.addMissingPreset('${current_theme_name}', '${name}')">&nbsp;</div></div>
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
  <div class="preview"><div onclick="NXThemesPresetManager.convertValueToPreset('${current_theme_name}', 'color', '${color}')" style="background-color: ${color}">&nbsp;</div></div>
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
<h3 class="nxthemesEditorFocus">Images that are not yet registered as presets ...</h3>

<table cellspacing="5" cellpadding="4" style="width: 100%">
<#assign count = 0 />
<#assign row = 1 />
<#assign row = (count % 10) +1 /> 

  <#if row == 0>
    <tr>
  </#if>
<#list images as image>
<td class="preset">
  <div class="preview"><div onclick="NXThemesPresetManager.convertValueToPreset('${current_theme_name}', 'image', '${image}')" style="background:${image}">&nbsp;</div></div>
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


<#else>


<#if selected_preset_group>
<!-- Palettes -->

<h2 class="nxthemesEditor" style="text-transform: uppercase">${selected_preset_group}</h2>

<table cellspacing="2" cellpadding="2" style="width: 100%">
<#assign count = 0 /> 
<#assign row = 1 /> 

<#list This.getGlobalPresets(selected_preset_group) as preset_info>
<#assign row = (count % 10) +1 /> 

  <#if row == 0>
    <tr>
  </#if>
<td class="preset">

<div class="preview" title="${preset_info.value}">
<ins class="model">
  {"id": "preset_${group}_${preset_info.name}",
   "type": "preset",
   "data": {
     "id": "${preset_info.id}",   
     "group": "${selected_preset_group}",
     "name": "${preset_info.name}",
     "editable": false,
     "copyable": true,
     "pastable": false,
     "deletable": false
     }
  }
</ins>
${preset_info.preview}</div>
<div class="name">${preset_info.name}</div>

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

</td></tr></table>

</div>

</div>

