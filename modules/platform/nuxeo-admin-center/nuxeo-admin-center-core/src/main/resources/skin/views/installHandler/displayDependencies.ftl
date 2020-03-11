<@extends src="base.ftl">

<@block name="header_scripts">
<script>

var subWin;
var installBaseUrl="${Root.path}/install/start/";
var uninstallBaseUrl="${Root.path}/uninstall/start/";
var downloadBaseUrl="${Root.path}/download/start/";
var autoMode = ${autoMode?xml};
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
    window.location.href="${Root.path}/install/start/${pkg.id}/?source=${source?xml}";
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
   $("#installAutoButton").css("display","inline-block");
   $("#manualModeCheckBox").removeAttr("checked");
 } else {
   $(".manualModeCmd").css("display","block");
   $("#installManualButton").css("display","inline-block");
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
     <h2>${Context.getMessage('label.displayDependencies.messages.warning')}</h2>

      <br/>
      <input type="checkbox" id="manualModeCheckBox" onClick="switchMode()">${Context.getMessage('label.displayDependencies.titles.manual')}</input>
      <br/>
      <#if (resolution.getRemovePackageIds()?size>0) >
      <h3>${Context.getMessage('label.displayDependencies.titles.toberemoved')}</h3>
      <table>
        <#list resolution.getRemovePackageIds() as pkgId>
          <tr>
          <td> ${pkgId} </td>
          <td><a href="javascript:rmPackage('${pkgId}')" class="manualModeCmd">${Context.getMessage('label.displayDependencies.links.manualremoval')}</a></td>
          </tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getUpgradePackageIds()?size>0) >
      <h3>${Context.getMessage('label.displayDependencies.titles.upgrade')}</h3>
      <table>
        <#list resolution.getUpgradePackageIds() as pkgId>
        <tr><td> ${pkgId} </td>
            <td><a href="javascript:installPackage('${pkgId}', true)" class="manualModeCmd">${Context.getMessage('label.displayDependencies.links.manualupgrade')}</a></td>
            <td><div id="progress_${pkgId}" class="progressDownloadContainer"> </div></td>
        </tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getLocalToInstallIds()?size>0) >
      <h3>${Context.getMessage('label.displayDependencies.titles.needinstall')}</h3>
      <table>
        <#list resolution.getLocalToInstallIds() as pkgId>
          <tr><td> ${pkgId} </td><td><a</a href="javascript:installPackage('${pkgId}', false)" class="manualModeCmd">${Context.getMessage('label.displayDependencies.links.manualinstall')}</a></td></tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getDownloadPackageIds()?size>0) >
      <h3>${Context.getMessage('label.displayDependencies.titles.needdownload')}</h3>
      <table>
        <#list resolution.getDownloadPackageIds() as pkgId>
          <tr>
          <td> ${pkgId} </td>
          <td><a href="javascript:installPackage('${pkgId}', true)" class="manualModeCmd">${Context.getMessage('label.displayDependencies.links.manualdownloadinstall')}</a></td>
          <td><div id="progress_${pkgId}" class="progressDownloadContainer"> </td>
          </tr>
        </#list>
      </table>
      </#if>

      <#if (resolution.getUnchangedPackageIds()?size>0) >
      <h3>${Context.getMessage('label.displayDependencies.titles.alreadyinstalled')}</h3>
      <table>
        <#list resolution.getUnchangedPackageIds() as pkgId>
          <tr><td> ${pkgId}</td></tr>
        </#list>
      </table>
      </#if>
   </div>

   <br/>
   <a href="javascript:downloadAllPackages()" id="downloadAllButton" class="button installButton" style="display:none">${Context.getMessage('label.displayDependencies.links.downloadall')}</a>
   <a href="${Root.path}/packages/${source?xml}" class="button"> Cancel </a> &nbsp;
   <a href="${Root.path}/install/bulkRun/${pkg.id}/?source=${source?xml}" class="button installButton" id="installAutoButton">${Context.getMessage('label.displayDependencies.links.install.start')} ${pkg.id} ${Context.getMessage('label.displayDependencies.links.install.stop')}</a>
   <a href="${Root.path}/install/start/${pkg.id}/?source=${source?xml}" class="button installButton" id="installManualButton">${Context.getMessage('label.displayDependencies.links.continue')} ${pkg.id} </a>

  </div>

</@block>
</@extends>