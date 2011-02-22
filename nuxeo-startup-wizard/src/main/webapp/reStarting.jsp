<%@page import="org.nuxeo.wizard.helpers.ServerController"%>
<%@ include file="includes/header.jsp"%>

<fmt:message key="label.restart.wait"/>

<%@ include file="includes/footer.jsp"%>

<%
    // do the actual restart once we have displayed the waiting page
    ServerController.restart(ctx);
%>
