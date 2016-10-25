<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
   <h3>${Context.getMessage('label.dependencyError.title.start')} ${pkg.title} (${pkg.id}) ${Context.getMessage('label.dependencyError.title.end')}</h3>

    <div class="installErrorTitle">
         ${Context.getMessage('label.dependencyError.message')}<br/>
         ${Context.getMessage('label.dependencyError.message2')}
    </div>
    <br/>
    <br/>
    <ul class="installErrors">
       ${resolution.toString()}
    </ul>
    <br/>
    <br/>
    <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.dependencyError.buttons.cancel')}</a>
  </div>

</@block>
</@extends>