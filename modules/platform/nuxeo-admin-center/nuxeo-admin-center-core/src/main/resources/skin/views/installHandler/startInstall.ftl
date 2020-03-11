<@extends src="base.ftl">

<@block name="header_scripts">
<script>
function closePopup() {
 self.close();
}
</script>
</@block>

<@block name="body">

  <div class="genericBox">

   <h1>${Context.getMessage('label.startInstall.title')} ${pkg.title} (${pkg.id}) </h1>

   <#if status.hasWarnings()>
      <div class="installWarningsTitle">
	 ${Context.getMessage('label.startInstall.message.haswarning')}
      </div>
      <ul class="installWarnings">
        <#list status.getWarnings() as warning>
          <li> ${warning} </li>
        </#list>
      </ul>
   </#if>

    <#if status.hasErrors()>
      <div class="installErrorsTitle">
      ${Context.getMessage('label.startInstall.message.haserror')}
      </div>
      <ul class="installErrors">
        <#list status.getErrors() as error>
          <li> ${error} </li>
        </#list>
      </ul>
    </#if>

   <#if installTask.isRestartRequired()>
     <div>
         ${Context.getMessage('label.startInstall.message.restartrequired')}
     </div>
   </#if>

   <br/>

   <#if needWizard>
      ${Context.getMessage('label.startInstall.message.clickstart')}<br/><br/><br/>
      <a href="${Root.path}/install/form/${pkg.id}/0?source=${source?xml}" class="button installButton">${Context.getMessage('label.startInstall.buttons.start')}</a>
   </#if>

   <#if !needWizard>
      ${Context.getMessage('label.startInstall.message.clickstartnowizard')} <br/><br/><br/>
      <a href="${Root.path}/install/run/${pkg.id}?source=${source?xml}" class="button installButton">${Context.getMessage('label.startInstall.buttons.start')}</a>
   </#if>

   &nbsp;
   <#if source=="installer">
     <a href="javascript:closePopup()" class="button installButton">${Context.getMessage('label.startInstall.buttons.cancel')}</a>
   <#else>
     <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.startInstall.buttons.cancel')}</a>
   </#if>
  </div>

</@block>
</@extends>