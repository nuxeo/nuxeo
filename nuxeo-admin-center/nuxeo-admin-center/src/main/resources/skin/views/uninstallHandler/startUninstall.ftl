<@extends src="base.ftl">

<@block name="header_scripts">

</@block>

<@block name="body">

 <div class="genericBox">

 		<h1> Uninstall of ${pkg.title} (${pkg.id}) </h1>

 		<#if status.hasWarnings()>
   		 <div class="installWarningsTitle">
    	   <p>Some warnings where found when checking the package:</p>
   		 	 <ul class="installWarnings">
   	  	 		<#list status.getWarnings() as warning>
      	 		 <li> ${warning} </li>
     			  </#list>
   		 	 </ul>
   		 </div>
 		</#if>

 		<#if uninstallTask.isRestartRequired()>
   		<div>
       After installation, you will need to restart your server.
   		</div>
 		</#if>

 		<br/>
    Click the start link to start the uninstall process. <br/><br/><br/>
    <a href="${This.path}/run/${pkg.id}?source=${source}" class="installButton"> Start </a>

 		 &nbsp; <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a>
  </div>

</@block>
</@extends>