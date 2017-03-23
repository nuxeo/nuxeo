<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

 <div class="genericBox">

    <h1> ${Context.getMessage('label.uninstall.uninstallOf')} ${pkg.title} (${pkg.id}) </h1>

    <#if status.hasWarnings()>
        <div class="installWarningsTitle">
        <p>${Context.getMessage('label.startInstall.message.haswarning')}:</p>
          <ul class="installWarnings">
              <#list status.getWarnings() as warning>
              <li> ${warning} </li>
            </#list>
          </ul>
        </div>
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

    <#if uninstallTask.isRestartRequired()>
      <div>
       ${Context.getMessage('label.simpleListing.titles.restartlink')}
      </div>
    </#if>

    <br/>
    ${Context.getMessage('label.uninstall.start')} <br/><br/><br/>
    <a href="${This.path}/run/${pkg.id}?source=${source?xml}" class="button installButton"> ${Context.getMessage('label.startInstall.buttons.start')} </a>
      &nbsp; <a href="${Root.path}/packages/${source?xml}" class="button installButton"> ${Context.getMessage('label.startInstall.buttons.cancel')} </a>
  </div>

</@block>
</@extends>