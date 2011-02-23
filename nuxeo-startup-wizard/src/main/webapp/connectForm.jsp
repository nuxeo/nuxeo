<%@ include file="includes/header.jsp" %>

<%
String cbUrl = (String) request.getAttribute("callBackUrl");

String formUrl = "https://connect.nuxeo.com/nuxeo/site/connect/trial/form";
formUrl = contextPath + "/jsp/fakeConnectForm.jsp";
formUrl = formUrl + "?WizardCB=" + cbUrl;

boolean showRegistrationForm = !ctx.isConnectRegistrationDone();

%>
<script>
function setSize() {
 $('#connectForm').css('height','450px');
 $('#connectForm').css('display','block');
}
</script>

<% if (showRegistrationForm) { %>

<iframe id="connectForm" src="<%=formUrl%>" onload="setSize()" width="100%" scrolling="no" marginwidth="0" marginheight="0" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; display:none"></iframe>

<!--
<input type="button" class="glossyButton" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<input type="button" class="glossyButton" value="<fmt:message key="label.action.skip"/>" onclick="navigateTo('<%=currentPage.next().next().getAction()%>');"/>
 -->
<% } else { %>

<h2> <fmt:message key="label.connectFinish.ok" /> </h2>

<input type="button" class="glossyButton" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<input type="button" class="glossyButton" value="<fmt:message key="label.action.next"/>" onclick="navigateTo('<%=currentPage.next().next().getAction()%>');"/>

<%} %>


<%@ include file="includes/footer.jsp" %>