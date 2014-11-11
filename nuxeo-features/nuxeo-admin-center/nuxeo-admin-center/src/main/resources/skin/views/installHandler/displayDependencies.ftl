<@extends src="base.ftl">

<@block name="header_scripts">
<script>

var subWin;
var installBaseUrl="${Root.path}/install/start/";
var uninstallBaseUrl="${Root.path}/uninstall/start/";
var downloadBaseUrl="${Root.path}/download/start/";
var autoMode = ${autoMode};
var lastDownloadStatus;

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
    // refresh
    window.location.href="${Root.path}/install/start/${pkg.id}/?source=${source}";
  }
}

function updateProgress(pkgId, progress) {
  var progressBarContainer = $(jqEscapeId("progress_" +  pkgId));
  if (progressBarContainer!=null) {
     progressBarContainer.html("");
     //var progressBar = $("<div>" + progress + "</div>");
     var progressBar = $("<div></div>");
     progressBar.addClass("progressDownload");
     progressBar.css("width", progress + "px");
     progressBarContainer.append(progressBar);
     progressBarContainer.css("display","block");
  }
}

function switchMode() {
  autoMode = ! autoMode;
  setMode();
}

function setMode() {
 if (autoMode) {
   $(".manualModeCmd").css("display","none");
   $("#installManualButton").css("display","none");
   $("#installAutoButton").css("display","inline");
   $("#manualModeCheckBox").removeAttr("checked");
 } else {
   $(".manualModeCmd").css("display","block");
   $("#installManualButton").css("display","inline");
   $("#installAutoButton").css("display","none");
   $("#manualModeCheckBox").attr("checked", "true");
 }
}

function displayDownloadButtonIfNeeded() {
  if ($(".progressDownloadContainer").size()>0 && ${resolution.nbPackagesToDownload}>0 ) {
     $("#downloadAllButton").css("display","inline");
     $("#installAutoButton").css("visibility","hidden");
  } else {
     $("#downloadAllButton").css("display","none");
     $("#installAutoButton").css("visibility","visible");
  }
}

$(document).ready(function() {
  setMode();
  displayDownloadButtonIfNeeded();
});

</script>
</@block>

<@block name="body">

  <div class="genericBox">
   <h1> Installation of ${pkg.title} (${pkg.id}) </h1>

   <div class="installWarningsTitle">
     <h2>The package you want to install requires some dependencies changes:</h2>

      <br/>
      <input type="checkbox" id="manualModeCheckBox" onClick="switchMode()"> Manual installation mode</input>
      <br/>
      <#if (resolution.getRemovePackageIds()?size>0) >
      <h3>Packages that need to be removed from your instance :</h3>
      <table>
        <#list resolution.getRemovePackageIds() as pkgId>
          <tr>
          <td> ${pkgId} </td>
          <td><A href="javascript:rmPackage('${pkgId}')" class="manualModeCmd">Manual removal</A></td>
          </tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getUpgradePackageIds()?size>0) >
      <h3>Already installed packages that need to be upgraded :</h3>
      <table>
        <#list resolution.getUpgradePackageIds() as pkgId>
        <tr><td> ${pkgId} </td>
            <td><A href="javascript:installPackage('${pkgId}', true)" class="manualModeCmd">Manual upgrade</A></td>
            <td><div id="progress_${pkgId}" class="progressDownloadContainer"> </div></td>
        </tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getInstallPackageIds()?size>0) >
      <h3>Already downloaded packages that need to be installed :</h3>
      <table>
        <#list resolution.getInstallPackageIds() as pkgId>
          <tr><td> ${pkgId} </td><td><A href="javascript:installPackage('${pkgId}', false)" class="manualModeCmd">Manual installation</A></td></tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getDownloadPackageIds()?size>0) >
      <h3>New packages that need to be  downloaded and installed :</h3>
      <table>
        <#list resolution.getDownloadPackageIds() as pkgId>
          <tr>
          <td> ${pkgId} </td>
          <td><A href="javascript:installPackage('${pkgId}', true)" class="manualModeCmd">Manual download and install</A></td>
          <td><div id="progress_${pkgId}" class="progressDownloadContainer"> </td>
          </tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getUnchangedPackageIds()?size>0) >
      <h3>Dependencies that are already installed on your instance and won't be changed :</h3>
      <table>
        <#list resolution.getUnchangedPackageIds() as pkgId>
          <tr><td> ${pkgId}</td></tr>
        </#list>
      </table>
      </#if>
   </div>

   <br/>
   <A href="javascript:downloadAllPackages()" id="downloadAllButton" class="installButton" style="display:none"> Download all packages </A>
   <a href="${Root.path}/packages/${source}" class="installButton"> Cancel </a> &nbsp;
   <A href="${Root.path}/install/bulkRun/${pkg.id}/?source=${source}" class="installButton" id="installAutoButton"> Installation of package ${pkg.id} and dependencies </a>
   <A href="${Root.path}/install/start/${pkg.id}/?source=${source}" class="installButton" id="installManualButton"> Continue installation of package ${pkg.id} </a>

  </div>

</@block>
</@extends>