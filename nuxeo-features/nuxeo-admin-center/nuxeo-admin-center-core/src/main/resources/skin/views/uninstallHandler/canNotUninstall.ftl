<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
    <h3> Uninstallation of ${pkg.title} (${pkg.id}) is not possible </h3>

    <div class="installErrorTitle">
       Some errors were found when checking the package.<br/>
       Uninstall process can not continue.
    </div>
    <ul class="installErrors">
      <#list status.getErrors() as error>
        <li> ${error} </li>
      </#list>
    </ul>
    <br />
    <a href="${Root.path}/packages/${source?xml}" class="button installButton"> Cancel </a>
  </div>

</@block>
</@extends>