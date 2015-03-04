<@extends src="base.ftl">

<@block name="header_scripts">
</@block>

<@block name="body">

 <div class="successfulDownloadBox">
     <h3> Uninstall of ${pkg.title} (${pkg.id}) completed </h3>


    <#if uninstallTask.isRestartRequired()>
       <div>
         You will need to restart your server to complete the uninstall.
         <br/>
         <form method="POST" action=""${This.path}/restart">
         Click on the restart button to restart the server now <input type="submit" value="Restart"/>
         </form>
       </div>
    </#if>

    <br/>

    <a href="${Root.path}/packages/${source?xml}" class="button installButton"> Finish </a>
 </div>

</@block>
</@extends>