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
   <h1> You have selected for installation the following packages</h1>
       <br/>
       <p>Since installation of all packages will be done automatically, you are advised to read the warnings and package description before starting the installation process.</p>
       <br/>
       <div style="width:100%;height:300px;overflow:scroll">
       <#list pkgIds as pkgId>
         <hr/>
         <h2>${pkgId}</h2>
         <#if warns[pkgId_index]??>
           <div class="installWarnings">${warns[pkgId_index]}</div>
         </#if>
         <div style="white-space:pre">${descs[pkgId_index]}</div>
       </#list>
       </div>
   <#if (rmPkgIds?size>0) >
    <h1> The following packages will be uninstalled </h1>
   <ul>
       <#list rmPkgIds as pkgId>
       <li>${pkgId}</li>
       </#list>
   </ul>
   </#if>
   <br/><br/>
   <a href="${This.path}/bulkRun/${pkgId?xml}/?source=${source?xml}&confirm=true" class="button installButton"> Confirm install </a>
  </div>
</@block>
</@extends>