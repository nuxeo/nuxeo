<%@page import="org.nuxeo.wizard.helpers.ServerController"%>
<%@ include file="includes/header.jsp"%>

RESTARTING, PLEASE WAIT...<br /><br />
Your browser will reload automatically when Nuxeo is ready.

<%@ include file="includes/footer.jsp"%>

<%
    // do the actual restart once we have displayed the waiting page
    ServerController.restart(ctx);
%>
