<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

  <div class="genericBox">
    <p>${Context.getMessage('label.confirmDownload.message')} ${pkg.id}.</p>

    <table>
      <tr>
        <td class="labelColumn">${Context.getMessage('label.confirmDownload.titles.package.title')}</td>
        <td> ${pkg.title} </td>
      </tr>
      <tr>
        <td class="labelColumn">${Context.getMessage('label.confirmDownload.titles.package.vendor')}</td>
        <td> ${pkg.vendor} </td>
      </tr>
      <tr>
        <td class="labelColumn">${Context.getMessage('label.confirmDownload.titles.package.description')}</td>
        <td style="white-space:pre-line"> ${pkg.description} </td>
      </tr>
    </table>

    <div class="alignCenter">
      <a class="button" href="${Root.path}/download/start/${pkg.id}?source=${source?xml}">${Context.getMessage('label.confirmDownload.links.confirm')}</a>
    </div>

  </div>

</@block>
</@extends>