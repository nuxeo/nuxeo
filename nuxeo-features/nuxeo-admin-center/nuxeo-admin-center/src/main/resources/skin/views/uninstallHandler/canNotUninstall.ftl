<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

 <h1> Installation of ${pkg.title} (${pkg.id}) is not possible </h1>

  <div class="installErrorTitle">
       Some errors where found when checking the package.<br/>
       Uninstall process can not continue.
  </div>
  <ul class="installErrors">
    <#list status.getErrors() as error>
      <li> ${error} </li>
    </#list>
  </ul>

  <A href="${Root.path}/packages/${source}" class="installButton"> Cancel </A>

</@block>
</@extends>