<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

 <br/>
 <br/>
 <br/>

 <h1> Uninstall of ${pkg.title} (${pkg.id}) </h1>

 <#if status.hasWarnings()>
    <div class="installWarningsTitle">
       Some warnings where found when checking the package
    </div>
    <ul class="installWarnings">
      <#list status.getWarnings() as warning>
        <li> ${warning} </li>
      </#list>
    </ul>
 </#if>

 <#if uninstallTask.isRestartRequired()>
   <div>
       After installation, you will need to restart your server.
   </div>
 </#if>

 <br/>
    Click the start link to start the uninstall process. <br/><br/><br/>
    <A href="${This.path}/run/${pkg.id}?source=${source}" class="installButton"> Start </A>

  &nbsp; <A href="${Root.path}/packages/${source}" class="installButton"> Cancel </A>

</@block>
</@extends>