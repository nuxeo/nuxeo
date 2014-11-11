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
  return window.confirm("Restart Nuxeo Server now ?");
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
    <td> ${pkg.name} </td>
    <td> ${pkg.title} </td>
    <td> ${pkg.version} &nbsp;</td>
    <td> ${pkg.targetPlatform} &nbsp;</td>
    <td> ${pkg.type} </td>
    <#if showCommunityInfo>
      <td>  rate : ${pkg.rating} | downloads : ${pkg.downloadsCount} | ${pkg.commentsNumber} comments
      </td>
    </#if>
    <#if showCommunityInfo>
      <td>  </td>
    </#if>
    <td> ${This.getStateLabel(pkg)} </td>
    <td class="alignCenter">
         <#if This.canDownload(pkg)>
           <a class="button download" href="${Root.path}/download/start/${pkg.id}?source=${source}"> Download </a>
         </#if>
         <#if This.canInstall(pkg)>
           <a class="button install" href="${Root.path}/install/start/${pkg.id}?source=${source}"> Install </a>
         </#if>
         <#if This.canUnInstall(pkg)>
           <a class="button uninstall" href="${Root.path}/uninstall/start/${pkg.id}?source=${source}"> Uninstall </a>
         </#if>
         <#if This.needsRestart(pkg)>
           <a class="button restartNeeded" onclick="return confirmRestart()" href="${Root.path}/restartView" target="_top" title="Installation will be completed on next restart">Restart&nbsp;required</a>
         </#if>
    </td>
  </tr>

 </#list>

</table>

</@block>
</@extends>