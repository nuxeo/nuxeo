<@extends src="base.ftl">

<@block name="header_scripts">

<script>
 var detailIdx=0;

 function toggleTd(td) {
  $(td).toggleClass('opentd');
  $(td).toggleClass('closetd');
 }

 function toggleDetails(pkgId, td) {
    var rowItem = $("tr").filter(function() { return this.id=='row_' + pkgId;})[0];
    var newRowId = 'pkgDetailRow_' + pkgId;
    toggleTd(td);
    var existingRowItem = $("tr").filter(function() { return this.id==newRowId;});
    if (existingRowItem.length>0) {
       $(existingRowItem[0]).remove();
       return;
    }
    detailIdx+=1;
    var newRow="<tr class='detailRow' id='" + newRowId ;
    newRow = newRow + "'><td>&nbsp;</td><td id='pkgDetailContent" + detailIdx;
    newRow = newRow + "' colspan='9'><img src='${Root.path}/skin/images/big_loading.gif' alt='loading package details...'></td></tr>";
    $(newRow).insertAfter($(rowItem));

    var targetUrl = "/nuxeo/site/connectClient/packages/details/" + pkgId;

    $.get(targetUrl, function(data) {
      $('#pkgDetailContent' + detailIdx).html(data);
    });
 }

 function showRealLogo(pkgid){
  var defaultImage = $(".pkgLogo").filter(function() { return this.id==('placeholder-' + pkgid);})[0]
  var realImage = $(".pkgLogo").filter(function() { return this.id==('logo-' + pkgid);})[0]
  $(defaultImage).css("display","none");
  $(realImage).css("display","block");
 }

 function fetchComments(pkgId) {
   var targetUrl="${This.getConnectBaseUrl()}marketplace/package/" + pkgId + "/comments";
   $.get(targetUrl, function(data) {
     var commentArea = $(".commentArea").filter(function() { return this.id==('commentArea-' + pkgId);})[0]
     $(commentArea).html(data);
    });
 }

 function confirmRestart() {
  return window.confirm("${Context.getMessage('label.simpleListing.messages.restart')}");
 }

</script>

</@block>

<@block name="body">

 <#if pkgs?size==0>
   <div class="infoMessage">
     ${Context.getMessage("label.package.listing.noPackage")}
   </div>
 </#if>

<table width="100%" class="packageListing">

 <#list pkgs as pkg>
 <#assign rowCss = (pkg_index % 2 == 0)?string("row_even","row_odd")/>
  <tr class="${rowCss}" id="row_${pkg.id}">
    <td class="opentd" onclick="javascript:toggleDetails('${pkg.id}', this)">&nbsp;&nbsp;&nbsp;</A> </td>
    <td> ${pkg.id} </td>
    <!--<td> ${pkg.name} </td>-->
    <td> ${pkg.title} </td>
    <td> ${pkg.version} &nbsp;</td>
    <td> ${pkg.targetPlatform} &nbsp;</td>
    <td> ${pkg.type} </td>
    <#if showCommunityInfo && This.canDownload(pkg)>
      <td>${Context.getMessage('label.simpleListing.downloadcount')} : ${pkg.downloadsCount}
      </td>
    <#else>
      <td>  </td>
    </#if>

    <#if This.getStateLabel(pkg) = "downloaded">
      <td>${Context.getMessage('label.pkgDetails.pkgState.downloaded')}</td>
    <#elseif This.getStateLabel(pkg) = "downloading">
      <td>${Context.getMessage('label.pkgDetails.pkgState.downloading')}</td>
    <#elseif This.getStateLabel(pkg) = "installed">
      <td>${Context.getMessage('label.pkgDetails.pkgState.installed')}</td>
    <#elseif This.getStateLabel(pkg) = "installing">
      <td>${Context.getMessage('label.pkgDetails.pkgState.installing')}</td>
    <#elseif This.getStateLabel(pkg) = "remote">
      <td>${Context.getMessage('label.pkgDetails.pkgState.remote')}</td>
    <#elseif This.getStateLabel(pkg) = "started">
      <td>${Context.getMessage('label.pkgDetails.pkgState.started')}</td>
    <#elseif This.getStateLabel(pkg) = "unknown">
      <td>${Context.getMessage('label.pkgDetails.pkgState.unknown')}</td>
    <#else>
      <td>${This.getStateLabel(pkg)}</td>
    </#if>

    <td>
         <#if This.canDownload(pkg)>
           <a class="button download" href="${Root.path}/download/start/${pkg.id}?source=${source?xml}&amp;filterOnPlatform=${filterOnPlatform}&amp;type=${type}&amp;onlyRemote=${onlyRemote}">${Context.getMessage('label.simpleListing.links.download')}</a>
         </#if>
         <#if This.registrationRequired(pkg)>${Context.getMessage('label.simpleListing.messages.registrationrequired')}</#if>
         <#if This.canCancel(pkg)>
           <a class="button cancel" href="${Root.path}/download/cancel/${pkg.id}?source=${source?xml}">${Context.getMessage('label.simpleListing.links.cancel')}</a>
         </#if>
         <#if This.canInstall(pkg)>
           <a class="button install" href="${Root.path}/install/start/${pkg.id}?source=${source?xml}">${Context.getMessage('label.simpleListing.links.install')}</a>
           <#if This.canRemove(pkg)>
             <a class="button remove" href="${Root.path}/remove/start/${pkg.id}?source=${source?xml}">${Context.getMessage('label.simpleListing.links.remove')}</a>
           </#if>
         </#if>
         <#if This.canUnInstall(pkg)>
           <a class="button uninstall" href="${Root.path}/uninstall/start/${pkg.id}?source=${source?xml}&amp;filterOnPlatform=${filterOnPlatform?xml}">${Context.getMessage('label.simpleListing.links.uninstall')}</a>
           <#if This.canUpgrade(pkg)>
             <a class="button upgrade" href="${Root.path}/install/start/${pkg.id}?source=${source?xml}">${Context.getMessage('label.simpleListing.links.upgrade')}</a>
           </#if>
         </#if>
         <#if This.needsRestart(pkg)>
           <#if pkg.getState() == 5 >
             <a class="button restartNeeded" onclick="return confirmRestart()" href="${Root.path}/restartView" target="_top" title="${Context.getMessage('label.simpleListing.titles.restartlink')}">${Context.getMessage('label.simpleListing.links.restart')}</a>
           <#else>
             <a class="button restartNeeded" onclick="return confirmRestart()" href="${Root.path}/restartView" target="_top" title="${Context.getMessage('label.simpleListing.titles.restartlink2')}">${Context.getMessage('label.simpleListing.links.restart')}</a>
           </#if>
         </#if>
    </td>
  </tr>

 </#list>

</table>

</@block>
</@extends>