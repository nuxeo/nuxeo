<@extends src="base.ftl">

<@block name="header_scripts">
<script>

var subWin;
var installBaseUrl="${Root.path}/install/start/";
var uninstallBaseUrl="${Root.path}/uninstall/start/";
var downloadBaseUrl="${Root.path}/download/start/";

function display(url) {
 if (subWin!=null) {
   try {
	   subWin.focus();
	   subWin.close();
	   }
   catch(err) {
      // NOP
   }
 }
 subWin = window.open(url,"Nuxeo Admin Center Installation","width=640,height=480");
}

function installPackage(pkgId,download) {
 if (download) {
   var url = downloadBaseUrl + pkgId + "?install=true&depCheck=false&source=installer";
   display(url);
 } else {
   var url = installBaseUrl + pkgId + "?depCheck=false&source=installer";
   display(url);
 }
}

function rmPackage(pkgId) {
   var url = uninstallBaseUrl + pkgId + "?depCheck=false&source=installer";
   display(url);
}
</script>
</@block>

<@block name="body">

  <div class="genericBox">

   <h1> Installation of ${pkg.title} (${pkg.id}) </h1>

   <div class="installWarningsTitle">
     The package you want to install has some dependencies :

      <#if (resolution.getRemovePackageIds()?size>0) >
      <br/><br/>
      Packages that need to be removed from your instance :
      <ul class="installWarning">
        <#list resolution.getRemovePackageIds() as pkgId>
          <li> <A href="javascript:rmPackage('${pkgId}')">${pkgId}</A></li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getUpgradePackageIds()?size>0) >
      <br/><br/>
      Already installed packages that need to be upgraded :
      <ul class="installInfo">
        <#list resolution.getUpgradePackageIds() as pkgId>
          <li> <A href="javascript:installPackage('${pkgId}', true)">${pkgId}</A></li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getInstallPackageIds()?size>0) >
      <br/><br/>
      Already downloaded packages that need to be installed :
      <ul class="installInfo">
        <#list resolution.getInstallPackageIds() as pkgId>
          <li> <A href="javascript:installPackage('${pkgId}', false)">${pkgId}</A></li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getDownloadPackageIds()?size>0) >
      <br/><br/>
      New packages that need to be  downloaded and installed :
      <ul class="installInfo">
        <#list resolution.getDownloadPackageIds() as pkgId>
          <li> <A href="javascript:installPackage('${pkgId}', true)">${pkgId}</A></li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getUnchangedPackageIds()?size>0) >
      <br/><br/>
	  Dependencies that are already installed on your instance and won't be changed :
      <ul class="installWarning">
        <#list resolution.getUnchangedPackageIds() as pkgId>
          <li> ${pkgId}</li>
        </#list>
      </ul>
      </#if>

   </div>

   <br/><br/>
   <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a> &nbsp;
   <A href="${Root.path}/install/start/${pkg.id}/?source=${source}"class="installButton"> Continue installation of package ${pkg.id} </a>

  </div>

</@block>
</@extends>