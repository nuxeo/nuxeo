<%@ include file="includes/header.jsp" %>
<%@page import="org.nuxeo.wizard.download.Preset"%><h1><fmt:message key="label.packagesSelection" /></h1>

<%
    String baseUrl = ctx.getBaseUrl();
    if (baseUrl == null) {
        baseUrl = "/nuxeo/";
    }
    DownloadablePackageOptions options = PackageDownloader.instance().getPackageOptions();
%>

<script src="<%=contextPath%>/scripts/packagesSelection.js"></script>

<script language="javascript">


  $(document).ready(function () {
    nuxeo.initPackagesSelection('<%=baseUrl%>PackageOptionsResource');
  });
</script>

<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.packagesSelection.description"/> <br/>
</span>
<%
    String presetClass = "display:none";
    if ("true".equals(request.getParameter("showPresets"))) {
        presetClass = "";
    }
%>

<%@ include file="includes/feedback.jsp" %>

<span style="<%=presetClass%>" id="hiddenPresets">
  <div class="presetContainer"> <span class="presetLabel"><fmt:message key="label.packagesSelection.presets"/> :</span>
  <%for (Preset preset : options.getPresets()) { %>
    <span class="presetBtn" id="preset_<%=preset.getId()%>"
          onclick="usePreset(<%=preset.getPkgsAsJsonArray()%>)"><%=preset.getLabel()%> </span>
  <%} %>
  </div>
  </span>
<br/>
<div id="tree"></div>

<span class="screenExplanations">
<fmt:message key="label.packagesSelection.explanations"/> <br/>
</span>

<%@ include file="includes/prevnext.jsp" %>
<%@ include file="includes/footer.jsp" %>
