<%@page import="java.util.TreeMap"%>
<%@ include file="includes/header.jsp" %>


<h1><fmt:message key="label.recapScreen" /></h1>

<form id="wizardform" action="<%=contextPath%>/<%=currentPage.getAction()%>" method="POST">
<span class="screenDescription">
<fmt:message key="label.recapScreen.description" />
<fmt:message key="label.recapScreen.explanations" />
</span>
<%
boolean connectOK = ctx.isConnectRegistrationDone();
%>
  <div class="screenExplanations">
  <table>
  <%Map<String,String> changedParams = collector.getChangedParameters();
  Map<String, String> sortedParams = new TreeMap<String, String>(collector.getConfigurationParams());
  for (String pName : sortedParams.keySet()) {
      String label = "label."+pName;%>
    <tr>
      <%if (changedParams.containsKey(pName) ||
              pName.startsWith("nuxeo.db") && !"default".equals(collector.getConfigurationParam(pName))) {%>
      <td class="bold">
      <%} else {%>
      <td>
      <%}%>
      <fmt:message key="<%=label%>"/></td>
      <%if (changedParams.containsKey(pName) ||
              pName.startsWith("nuxeo.db") && !"default".equals(collector.getConfigurationParam(pName))) {%>
      <td class="bold highlighted">
      <%} else {%>
      <td>
      <%}%>
      <%if (pName.equals("nuxeo.db.password") || pName.equals("nuxeo.http.proxy.password") || pName.equals("mail.transport.password")) {%>
        ******
      <%} else {%>
        <%=collector.getConfigurationParam(pName)%></td>
      <%}%>
    </tr>
  <%} %>
  </table>
 </div>

 <%if (connectOK) {
    Map<String,String> connectMap = ctx.getConnectMap();%>
    <div class="screenExplanations">
      <div class="bold"><fmt:message key="label.connectFinish.ok" /></div>
      <div id="CLID"><%=connectMap.get("CLID")%></div>
    </div>
 <%}%>

 <div class="buttonContainer">
<%if (currentPage.prev()!=null) { %>
 <input type="button" id="btnPrev" class="glossyButton" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<%}%>
 <input type="submit" id="btnNext" class="glossyButton" value="<fmt:message key="label.action.finish"/>"/>
</div>

</form>

<%@ include file="includes/footer.jsp" %>
