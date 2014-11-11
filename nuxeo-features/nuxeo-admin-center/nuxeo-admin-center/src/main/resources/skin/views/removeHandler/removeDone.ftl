<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

 <div class="successfulDownloadBox">
        <h3>${Context.getMessage('label.removeDone.message.start')} ${pkgId} ${Context.getMessage('label.removeDone.message.end')}</h3>
    <br/>
    <a href="${Root.path}/packages/${source}" class="button installButton">${Context.getMessage('label.removeDone.links.finish')}</a>
 </div>

</@block>
</@extends>