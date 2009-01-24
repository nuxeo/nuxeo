<div>
 
<!-- preset menu -->
<@nxthemes_view resource="preset-menu.json" />     
      
<div id="nxthemesPresetManager">

<h1 class="nxthemesEditor">Presets</h1>

<#assign presets = This.getCustomPresets(current_theme_name)>

<h2 class="nxthemesEditor">Theme: ${current_theme_name}</h2>

<table cellspacing="5" cellpadding="4" style="margin-bottom: 10px; width: 100%">
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
       {"label": "Color", "choice": "color"},
       {"label": "Background", "choice": "background"},
       {"label": "Font", "choice": "font"},
       {"label": "Image", "choice": "image"}
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
<h3 class="nxthemesEditor">Presets that must be registered ...</h3>
<table cellspacing="5" cellpadding="4" style="margin-bottom: 30px; width: 100%">
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
<h3 class="nxthemesEditor">Colors that are not yet registered as presets ...</h3>

<table cellspacing="5" cellpadding="4" style="margin-bottom: 30px; width: 100%">
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



<#list preset_groups as group>
<h2 class="nxthemesEditor" style="text-align: center">${group}</h2>

<table cellspacing="5" cellpadding="4" style="margin-bottom: 30px; width: 100%">
<#assign count = 0 /> 
<#assign row = 1 /> 

<#list This.getGlobalPresets(group) as preset_info>
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
     "group": "${group}",
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

</#list>

</div>

</div>

