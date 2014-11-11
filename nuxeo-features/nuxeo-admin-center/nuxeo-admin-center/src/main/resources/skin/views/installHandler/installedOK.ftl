<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">
  <div class="successfulDownloadBox">
   <h1> Installation of ${pkg.title} (${pkg.id}) completed </h1>
  
  
    <#if installTask.isRestartRequired()>
     <div>
         You will need to restart your server to complete the installation.
         <br/>
         <form method="POST" action=""${This.path}/restart">
         Click on the restart button to restart the server now <input type="submit" value="Restart"/>
         </form>Restart the server <a href="">now</a>.
     </div>
    </#if>
  
    <br/>
  
    <a href="${Root.path}/packages/${source}" class="installButton"> Finish </a>
  </div>
</@block>
</@extends>