<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
   <h3>${Context.getMessage('label.canNotInstall.title.start')} ${pkg.title} (${pkg.id}) ${Context.getMessage('label.canNotInstall.title.end')}</h3>

    <div class="installErrorTitle">
         ${Context.getMessage('label.canNotInstall.message')}<br/>
         ${Context.getMessage('label.canNotInstall.message2')}
    </div>
    <ul class="installErrors">
      <#list status.getErrors() as error>
        <li> ${error} </li>
      </#list>
    </ul>
    <br />
    <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.canNotInstall.buttons.cancel')}</a>
  </div>

</@block>
</@extends>