<%@ include file="includes/header.jsp" %>

<%@page import="java.util.List"%>
<%@page import="org.nuxeo.wizard.download.PendingDownload"%>
<%@page import="org.nuxeo.wizard.download.PendingDownloadStatus"%>

<h1><fmt:message key="label.packagesDownload" /></h1>

<%
String baseUrl = ctx.getBaseUrl();
if (baseUrl==null) {
    baseUrl="/nuxeo/";
}
boolean needDownload = false;
List<DownloadPackage> packages = PackageDownloader.instance().getSelectedPackages();
List<PendingDownload> downloads = PackageDownloader.instance().getPendingDownloads();
boolean downloadStarted = PackageDownloader.instance().isDownloadStarted();
boolean downloadCompleted = PackageDownloader.instance().isDownloadCompleted();
boolean downloadInProgress = PackageDownloader.instance().isDownloadInProgress();
String selectedPackageIds="";
%>

<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.packagesDownload.description" /> <br/>
</span>

<% if (!downloadStarted) { %>
<table class="screenExplanations">
<tr><td>
<ul>
<%for (DownloadPackage pkg : packages) {
    selectedPackageIds+=pkg.getId() + "|";
    // Skip virtual packages
    if (pkg.isVirtual()) { continue; }
%>
  <li><%=pkg.getLabel()%> <div class="detail"><%=pkg.getId()%> &nbsp;
  <%if (pkg.isLaterDownload()) {%>
    <fmt:message key="label.packagesDownload.laterDownload" />
  <%} else if (pkg.isAlreadyInLocal()) {%>
    <fmt:message key="label.packagesDownload.alreadyInLocal" />
  <%} else {
    needDownload = true;%>
    <fmt:message key="label.packagesDownload.toDownload" />
  <%} %></div>
  </li>
<%}%>
</ul>
</td>
</tr>

</table>
<% }%>

<script type="text/javascript">

function refreshDownloadTable() {
  setTimeout(function() { $("#downloadTable").load('<%=currentPage.getAction()%> #downloadTable', function() {
      if ($("#downloadInProgress").html()=="true") {
        refreshDownloadTable();
      }
      else if ($("#downloadCompleted").html()=="true") {
        $("#btnNext").css("display","inline");
      }
    })}, 1500);
}

$(document).ready(function(){
<%if (needDownload) {%>
   $("#btnDownload").css("display","inline");
<% };
  if (downloadCompleted || !needDownload) {%>
   $("#btnNext").css("display","inline");
<%};
  if (downloadInProgress) {%>
  refreshDownloadTable();
<%}%>

  $("#connectBannerIframe").attr("src","<%=currentPage.getAssociatedIFrameUrl()%>?pkgs=<%=selectedPackageIds%>");

});
</script>

<% if (downloadStarted) { %>
<div id="downloadTable">
<table>
<%for (PendingDownload dw : downloads) {%>
  <tr>
     <td> <%=dw.getPkg().getLabel()%> <br/>( <%=dw.getPkg().getFilename() %>)</td>
     <td>
     <%
     switch (dw.getStatus()) {
         case PENDING:
             %><fmt:message key="label.downloadStatus.PENDING"/><%
             break;
         case INPROGRESS:
             %><fmt:message key="label.downloadStatus.INPROGRESS"/><%
             break;
         case VERIFICATION:
             %><fmt:message key="label.downloadStatus.VERIFICATION"/><%
             break;
         case VERIFIED:
             %><fmt:message key="label.downloadStatus.VERIFIED"/>
             <img src="<%=contextPath%>/images/pkgok.png" height="18"/><%
             break;
         case ABORTED:
             %><fmt:message key="label.downloadStatus.ABORTED"/><%
             break;
         case COMPLETED:
             %><fmt:message key="label.downloadStatus.COMPLETED"/><%
             break;
         case CORRUPTED:
             %><fmt:message key="label.downloadStatus.CORRUPTED"/>
             <img src="<%=contextPath%>/images/broken.png" height="18"/><%
             break;
         case MISSING:
             %><fmt:message key="label.downloadStatus.MISSING"/>
             <img src="<%=contextPath%>/images/broken.png" height="18"/><%
             break;
     }%>
     </td>
     <td>
       <div style="width:200px:height:10px">
       <%
       switch (dw.getStatus()) {
          case PENDING:
              %>
              <img src="<%=contextPath%>/images/pause.png" height="18" title="Pending"/>
              <%
              break;
          case ABORTED:
          case CORRUPTED:
          case MISSING:
              %>
              <A href="#" onclick="navigateTo('<%=currentPage.getAction()%>?reStartDownload=<%=dw.getPkg().getId()%>');"><fmt:message key="label.downloadStatus.restartDownload"/></A>
              <%
              break;
          default:
              %>
         <div style="width:220px">
         <div class="downloadProgress" style="width:<%=2*dw.getProgress()%>px"></div>
         </div>
              <%
       }
       %>
       </div>
      </td>
      <td>
      <%if (dw.getStatus()==PendingDownloadStatus.INPROGRESS) {%>
        <%=dw.getProgress()%> %
      <%}%>
      </td>
  </tr>
<%}%>
</table>
<div style="display:none" id="downloadInProgress"><%=downloadInProgress%></div>
<div style="display:none" id="downloadCompleted"><%=downloadCompleted%></div>
<%if (downloadInProgress) {%>
<div id="downloadWaiter"><fmt:message key="label.downloadStatus.INPROGRESS"/></div>
<%}%>
</div>
<%}%>

 <div class="buttonContainer">
 <input type="button" class="glossyButton" id="btnPrev" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
 <input style="display:none" type="button" class="glossyButton" id="btnDownload" value="<fmt:message key="label.action.downloadStart"/>"  onclick="navigateTo('<%=currentPage.getAction()%>?startDownload=true');"/>
 <input style="display:none" type="submit" class="glossyButton" id="btnNext" value="<fmt:message key="label.action.next"/>"/>
 </div>

</form>
<%@ include file="includes/footer.jsp" %>
