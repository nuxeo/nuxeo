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
   <h1>${Context.getMessage('label.bulkInstallOnRestart.title.install')}</h1>

   <ul>
       <#list pkgIds as pkgId>
       <li>${pkgId}</li>
       </#list>
   </ul>

   <#if (rmPkgIds?size>0) >
   <h1>${Context.getMessage('label.bulkInstallOnRestart.title.uninstall')}</h1>
   <ul>
       <#list rmPkgIds as pkgId>
       <li>${pkgId}</li>
       </#list>
   </ul>
   </#if>

    <br/>
    <div>
		${Context.getMessage('label.bulkInstallOnRestart.message')}
    </div>

    <#if source=="installer">
      <a href="javascript:closePopup()" class="button installButton">${Context.getMessage('label.bulkInstallOnRestart.buttons.finish')}</a>
    <#else>
      <a href="${Root.path}/packages/${source?xml}" class="button installButton">${Context.getMessage('label.bulkInstallOnRestart.buttons.finish')}</a>
    </#if>
  </div>
</@block>
</@extends>