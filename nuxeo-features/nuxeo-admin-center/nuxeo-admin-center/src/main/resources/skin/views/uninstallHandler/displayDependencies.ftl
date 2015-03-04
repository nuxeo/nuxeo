<@extends src="base.ftl">

<@block name="header_scripts">
<script>

var subWin;
var uninstallBaseUrl="${Root.path}/uninstall/start/";
var autoMode = true;

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
 subWin = window.open(url,"Nuxeo Admin Center Uninstallation","width=640,height=480");
}

function rmPackage(pkgId) {
   var url = uninstallBaseUrl + pkgId + "?depCheck=false&source=installer";
   display(url);
}

function switchMode() {
  autoMode = ! autoMode;
  setMode();
}

function setMode() {
 if (autoMode) {
   $(".manualModeCmd").css("display","none");
   $("#uninstallManualButton").css("display","none");
   $("#uninstallAutoButton").css("display","inline-block");
   $("#manualModeCheckBox").removeAttr("checked");
 } else {
   $(".manualModeCmd").css("display","block");
   $("#uninstallManualButton").css("display","block");
   $("#uninstallAutoButton").css("display","none");
   $("#manualModeCheckBox").attr("checked", "true");
 }
}

$(document).ready(function() {
  setMode();
});

</script>
</@block>

<@block name="body">

  <div class="genericBox">
   <h1> Uninstallation of ${pkg.title} (${pkg.id}) </h1>

   <div class="installWarningsTitle">
     <h2>The package you want to uninstall requires some dependencies changes:</h2>

      <br/>
      <input type="checkbox" id="manualModeCheckBox" onClick="switchMode()"> Manual Uninstallation mode </input>
      <br/>
      <h3>Packages that need to be removed from your instance: </h3>
      <table>
        <#list pkgToRemove as pkgR>
          <tr>
          <td> ${pkgR.id} </td>
          <td><a href="javascript:rmPackage('${pkgR.id}')" class="manualModeCmd">Manual removal</a></td>
          </tr>
        </#list>
      </table>
   </div>

   <br/>
   <a href="${Root.path}/packages/${source?xml}" class="button"> Cancel </a> &nbsp;
   <a href="${Root.path}/uninstall/run/${pkg.id}/?source=${source?xml}"class="button installButton" id="uninstallAutoButton"> Uninstallation of package ${pkg.id} and dependent packages </a>
   <a href="${Root.path}/uninstall/start/${pkg.id}/?source=${source?xml}"class="button installButton" id="uninstallManualButton"> Continue uninstallation of package ${pkg.id} </a>

  </div>

</@block>
</@extends>