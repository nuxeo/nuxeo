
<#macro errorIcon field message>
  <#if This.hasErrors(field)><img src="${skinPath}/image/error.gif" title="${message}"></#if>
</#macro>

<#macro errors>
  <#if This.error??>
	<p><font color="red"><b>Errors: </b><ul>${This.error.xmlMessage}</ul></font></p>
  </#if>
</#macro>

<#macro ok>
<#if This.isOkEnabled()>
  <input type="button" class="wizardButton" value="Ok" onClick="this.form.action='${This.path}/ok'; this.form.submit();">
</#if>
</#macro>

<#macro next>
<#if This.isNextEnabled()>
  <input type="button" class="wizardButton" value="Next" onClick="this.form.action='${This.path}/next'; this.form.submit();">
</#if>
</#macro>

<#macro back>
<#if This.isBackEnabled()>
  <input type="button" class="wizardButton" value="Back" onClick="this.form.action='${This.path}/back'; this.form.submit();">
</#if>
</#macro>

<#macro cancel>
<#if This.isCancelEnabled()>
  <input type="button" class="wizardButton" value="Cancel" onClick="this.form.action='${This.path}/cancel'; this.form.submit();">
</#if>
</#macro>
