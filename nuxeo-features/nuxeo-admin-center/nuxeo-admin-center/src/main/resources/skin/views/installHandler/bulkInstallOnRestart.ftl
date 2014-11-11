<@extends src="base.ftl">

<@block name="header_scripts">
<script>
function closePopup() {
 self.close();
}
</script>
</@block>

<@block name="body">
  <div class="infoDownloadBox">
   <h1> You have scheduled installation of the following packages</h1>

   <ul>
       <#list pkgIds as pkgId>
       <li>${pkgId}</li>
       </#list>
   </ul>

   <#if (rmPkgIds?size>0) >
   <h1> You have scheduled uninstallation of the following packages</h1>
   <ul>
       <#list rmPkgIds as pkgId>
       <li>${pkgId}</li>
       </#list>
   </ul>
   </#if>

    <br/>
    <div>
     Installation will be completed on next server restart.
    </div>

    <#if source=="installer">
      <a href="javascript:closePopup()" class="installButton"> Finish </a>
    <#else>
      <a href="${Root.path}/packages/${source}" class="installButton"> Finish </a>
    </#if>
  </div>
</@block>
</@extends>