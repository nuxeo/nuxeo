<%@ include file="includes/header.jsp" %>
<%@page import="org.nuxeo.wizard.helpers.ConnectRegistrationHelper"%>
<%
String cbUrl = (String) request.getAttribute("callBackUrl");

String formUrl = "https://connect.nuxeo.com/nuxeo/site/connect/embeddedTrial/form";
// String formUrl = "https://connect-test.nuxeo.com/nuxeo/site/connect/embeddedTrial/form";
formUrl = formUrl + "?WizardCB=" + cbUrl;
formUrl = formUrl + "&source=wizard&pkg=" + ctx.getDistributionKey();

boolean showRegistrationForm = !ctx.isConnectRegistrationDone();

if (ConnectRegistrationHelper.isConnectRegistrationFileAlreadyPresent(ctx)) {
    showRegistrationForm = false;
}

%>
<script>
var connectFormLoaded=false;
function setSize() {
 $('#connectForm').css('height','500px');
 $('#connectForm').css('display','block');
 connectFormLoaded=true;
}

function handleFallbackIfNeeded() {
  if(!connectFormLoaded) {
  $('#fallback').css('display','block');
  }
}

window.setTimeout(handleFallbackIfNeeded, 25000);

</script>

<% if (showRegistrationForm) { %>

<iframe id="connectForm" src="<%=formUrl%>" onload="setSize()" width="100%" scrolling="no" marginwidth="0" marginheight="0" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; display:none"></iframe>

<div style="display:none" id="fallback">

<p>
<fmt:message key="label.connectForm.loadError1" />
</p>
<p>
<fmt:message key="label.connectForm.loadError2" />
</p>
<input type="button" id="btnNext" class="glossyButton" value="<fmt:message key="label.action.next"/>" onclick="navigateTo('<%=currentPage.next().next().getAction()%>');"/>

</div>

<% } else { %>

<h1> <fmt:message key="label.connectFinish.ok" /> </h1>
<div class="formPadding">
<fmt:message key="label.connectFinish.ok.details" />
<%@ include file="includes/prevnext.jsp" %>

<script>
  window.setTimeout(function() {navigateTo('<%=currentPage.next().getAction()%>')}, 3000);
</script>
<%} %>


<%@ include file="includes/footer.jsp" %>