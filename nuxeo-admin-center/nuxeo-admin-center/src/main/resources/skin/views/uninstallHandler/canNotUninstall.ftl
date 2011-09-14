<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
    <h1> Uninstallation of ${pkg.title} (${pkg.id}) is not possible </h1>

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
    <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a>
  </div>

</@block>
</@extends>