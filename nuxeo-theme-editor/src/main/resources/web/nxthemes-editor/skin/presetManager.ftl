<div>

<script type="text/javascript"><!--
window.scrollTo(0,0);
//--></script>

<div id="nxthemesPresetManager">

<h1 class="nxthemesEditor">Manage presets</h1>

<#list preset_groups as group>
<h3 style="padding: 2px 4px; background-color: #f6f6f6" class="nxthemesEditor">${group}</h3>

<table cellspacing="5" cellpadding="4" style="margin-bottom: 30px; width: 100%">
<#assign count = 0 /> 
<#list This.getGlobalPresets(group) as preset_info>
  <#if count % 10 == 0>
    <tr>
  </#if>
<td>

<div class="preview" title="${preset_info.value}">${preset_info.preview}</div>
<div class="name">${preset_info.name}</div>
</td>
  <#if count % 10 == 10>
    </tr>
  </#if>
  <#assign count = count + 1/>
</#list>
</table>

</#list>


<#list theme_names as theme_name>
<#assign presets = This.getCustomPresets(theme_name)>

<h3 style="padding: 2px 4px; background-color: #f6f6f6" class="nxthemesEditor">Theme: ${theme_name}</h3>

<table cellspacing="5" cellpadding="4" style="margin-bottom: 30px; width: 100%">
<#assign count = 0 />
<#list presets as preset_info>
<#assign count = 0 />
  <#if count % 10 == 0>
    <tr>
  </#if>
<td>

<div class="preview" title="${preset_info.value}">${preset_info.preview}</div>
<div class="name">${preset_info.name}</div>
</td>
  <#if count % 10 == 10>
    </tr>
  </#if>
  <#assign count = count + 1/>
</#list>
</table>

</#list>

</div>

</div>

