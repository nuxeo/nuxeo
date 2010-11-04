<#setting url_escaping_charset='UTF-8'>


<table style="width: 100%" cellspacing="0" cellpadding="0">

<tr>
<td style="width: 49%; vertical-align: top">

<div class="window">
<div class="title">General</div>
<div class="body">
<#if current_theme>
  <p class="nxthemesEditor">Theme name: <strong>${current_theme.name}</strong></p>
  <#if current_bank>
    <p class="nxthemesEditor">Resource bank: <strong>${current_bank.name}</strong></p>
  </#if>  
  <p class="nxthemesEditor">Theme source: <strong>${current_theme.src}</strong></p> 
</#if>
</div>
</div>

<div class="window">
<div class="title">Skin</div>
<div class="body">
  <#if current_bank && current_skin_name>
    <#assign current_skin=Root.getSkinInfo(current_bank.name, current_skin_name) />
    <#if current_skin>
    <p class="nxthemesEditor">Current skin: <strong>${current_skin.name}</strong>
    <div style="margin: 10px;">
      <img style="border: 1px solid #ccc;" src="${current_bank.connectionUrl}/${current_skin.collection}/style/${current_skin.resource}/preview"" />
    <div>
    </p>
    </#if>
  <#else>
    <p class="nxthemesEditor">You have not selected a theme skin yet.</p>
  </#if>
  <p class="nxthemesEditor">
    <button class="nxthemesActionButton"
     onclick="NXThemesEditor.manageSkins()">
     <#if current_skin_name>Change skin<#else>Choose a skin</#if></button>
  </p>
</div>
</div>

</td>

<td style="width: 2%">
</td>

<td style="width: 48%; vertical-align: top">

<div class="window">
<div class="title">Theme options</div>
<div class="body">
<#assign presets = This.getCustomPresets(current_theme_name, null)>
<#list presets as preset_info>
  <p class="nxthemesEditor">
    <strong title="${preset_info.description}">${preset_info.label}</strong>:
    ${preset_info.value}
  </p>
</#list>
  <p class="nxthemesEditor">
    <button class="nxthemesActionButton"
     onclick="NXThemesEditor.setThemeOptions()">Set theme options</button>
  </p>
</div>
</div>

<div class="window">
<div class="title">CSS</div>
<div class="body">

<#assign theme_skin = Root.getThemeSkin(current_theme.name) />
<#if theme_skin & theme_skin.customized>
 <p class="nxthemesEditor">You have customized the current skin</p>
<#else>
 <p class="nxthemesEditor">No CSS customizations have been made</p>
</#if>

  <p class="nxthemesEditor">
    <button class="nxthemesActionButton"
     onclick="NXThemesEditor.editCss()">Edit CSS</button>
  </p>

</div>
</div>

</td>
</tr>
</table>
