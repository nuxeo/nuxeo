<div>

<div id="nxthemesStyleManager" class="nxthemesScreen">

<h1 class="nxthemesEditor">Styles</h1>

<h2 class="nxthemesEditor">Unidentified presets</h2>

<table cellpadding="0" cellspacing="0" border="0">
<tr>
<th>&nbsp</th>
<th style="width: 10%">Theme</th>
<th style="width: 90%">Preset name</th>
</tr>
<#list theme_names as theme_name>
<#list This.getUnidentifiedPresetNames(theme_name) as name>
<tr>
  <td><img src="${skinPath}/img/theme-16.png" width="16" height="16" /></td>
  <td>${theme_name}</td>
  <td>${name}</td>
</tr>
</#list>
</#list>

</table>

</div>

