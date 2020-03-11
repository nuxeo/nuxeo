<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
    <h3>${Context.getMessage('label.installError.title')} ${e.message}.</h3>
    <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.installError.buttons.cancel')}</a>
  </div>
</@block>
</@extends>