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

function downloadAllPackages() {
  var allPkgIds = "${resolution.getAllPackagesToDownloadAsString()}";
  var url = "${Root.path}/download/startDownloads?pkgList=" + allPkgIds;

  $(document).ajaxError(function(e, jqxhr, settings, exception) {
  console.log("XHRError");
  console.log(exception);
  });

  $.get(url, function(data) {console.log("started"); refreshDownloadProgress(data);});
}

function jqEscapeId(myid) {
   return '#' + myid.replace(/(:|\.)/g,'\\$1');
 }

var lastDownloadStatus;

function refreshDownloadProgress(downloadStatus) {
  var updatedPkgIds=[];
  // update all progressbars
  for (var i = 0; i < downloadStatus.length; i++) {
    updateProgress(downloadStatus[i].pkgid, downloadStatus[i].progress);
    updatedPkgIds.push(downloadStatus[i].pkgid);
  }
  // put progressbar to 100% for  completed download
  if (lastDownloadStatus!=null && lastDownloadStatus.length && lastDownloadStatus.length>0) {
    for (var i = 0; i < lastDownloadStatus.length; i++) {
      if (updatedPkgIds.indexOf(lastDownloadStatus[i].pkgid)<0) {
        updateProgress(lastDownloadStatus[i].pkgid, 100);
      }
    }
  }
  if (downloadStatus.length>0) {
    lastDownloadStatus = downloadStatus;
    setTimeout(function() {
        $.get("${Root.path}/download/progressAsJSON", function(data) {
           refreshDownloadProgress(data);
        });
    }, 1000);
  } else {
    alert("All downloads completed!");
  }
}

function updateProgress(pkgId, progress) {
  var progressBarContainer = $(jqEscapeId("progress_" +  pkgId));
  if (progressBarContainer!=null) {
     progressBarContainer.html("");
     var progressBar = $("<div>" + progress + "</div>");
     progressBar.addClass("progressDownload");
     progressBar.css("width", progress + "px");
     progressBarContainer.append(progressBar);
     progressBarContainer.css("display","block");
  }
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
          <li> ${pkgId} <A href="javascript:rmPackage('${pkgId}')">Manual removal</A></li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getUpgradePackageIds()?size>0) >
      <br/><br/>
      Already installed packages that need to be upgraded :
      <ul class="installInfo">
        <#list resolution.getUpgradePackageIds() as pkgId>
          <li> ${pkgId} <A href="javascript:installPackage('${pkgId}', true)">Manual upgrade</A>
          <div id="progress_${pkgId}" class="progressDownloadContainer"> </div>
          </li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getInstallPackageIds()?size>0) >
      <br/><br/>
      Already downloaded packages that need to be installed :
      <ul class="installInfo">
        <#list resolution.getInstallPackageIds() as pkgId>
          <li> ${pkgId} <A href="javascript:installPackage('${pkgId}', false)">Manual installation</A>
          </li>
        </#list>
      </ul>
      </#if>

      <#if (resolution.getDownloadPackageIds()?size>0) >
      <br/><br/>
      New packages that need to be  downloaded and installed :
      <ul class="installInfo">
        <#list resolution.getDownloadPackageIds() as pkgId>
          <li> ${pkgId} <A href="javascript:installPackage('${pkgId}', true)">Manual download and install</A>
          <div id="progress_${pkgId}" class="progressDownloadContainer"> </div>
          </li>
        </#list>
      </ul>
      <A href="javascript:downloadAllPackages()"> Download all packages </A>
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