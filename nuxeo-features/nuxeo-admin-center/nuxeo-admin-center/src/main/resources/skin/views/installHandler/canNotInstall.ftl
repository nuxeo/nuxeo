<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="errorDownloadBox">
   <h3> Installation of ${pkg.title} (${pkg.id}) is not possible </h3>
  
    <div class="installErrorTitle">
         Some errors where found when checking the package.<br/>
         Installation process can not continue.
    </div>
    <ul class="installErrors">
      <#list status.getErrors() as error>
        <li> ${error} </li>
      </#list>
    </ul>
    <br />
    <a href="${Root.path}/packages/${source}" class="button installButton"> Cancel </a>
  </div>

</@block>
</@extends>