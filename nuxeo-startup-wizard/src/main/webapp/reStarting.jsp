<%@page import="org.nuxeo.wizard.helpers.ServerController"%>
<%@ include file="includes/header.jsp"%>

<div id="loading"><fmt:message key="label.restart.wait" /><br/><br/>
<img src="<%=contextPath%>/images/restart_waiter.gif" /></div>

<%@ include file="includes/footer.jsp"%>

<%
    // do the actual restart once we have displayed the waiting page
    ServerController.restart(ctx);
%>

<script type="text/javascript">
setTimeout(startPolling, 10000);
function startPolling() {
    var intId = setInterval(function isNuxeoReady() {
        $.get("<%=contextPath%>/login.jsp", function(data, textStatus) {
            window.location.href='<%=contextPath%>/';
        });
    }, 2000);
}
</script>