<div>

<script type="text/javascript"><!--
window.scrollTo(0,0);
//--></script>

<div id="nxthemesPresetManager">

<h1 class="nxthemesEditor">Manage presets</h1>

<table cellspacing="0" cellpadding="10" >
<#assign count = 0 /> 
<#list presets as preset_info>
  <#if count % 10 == 0>
    <tr>
  </#if>
<td>
<div class="name">${preset_info.name}</div>
<div class="preview" title="${preset_info.value}">${preset_info.preview}</div>
</td>
  <#if count % 10 == 10>
    </tr>
  </#if>
  <#assign count = count + 1/>
</#list>
</table>

</div>

</div>

