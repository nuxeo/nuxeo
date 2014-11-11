<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

  <div class="genericBox">
    <p>You are about to start download for package ${pkg.id}.</p>

    <table>
      <tr>
        <td class="labelColumn"> Package title </td>
        <td> ${pkg.title} </td>
      </tr>
      <tr>
        <td class="labelColumn"> Package vendor </td>
        <td> ${pkg.vendor} </td>
      </tr>
      <tr>
        <td class="labelColumn"> Package description </td>
        <td style="white-space:pre-line"> ${pkg.description} </td>
      </tr>
    </table>

    <div class="alignCenter">
      <a class="button" href="${Root.path}/download/start/${pkg.id}?source=${source}"> Confirm and Start the download </a>
    </div>

  </div>

</@block>
</@extends>