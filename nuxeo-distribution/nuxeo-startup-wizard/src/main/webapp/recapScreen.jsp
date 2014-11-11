<%@page import="java.util.TreeMap"%>
<%@ include file="includes/header.jsp" %>


<h1><fmt:message key="label.recapScreen" /></h1>

<form id="wizardform" action="<%=contextPath%>/<%=currentPage.getAction()%>" method="POST">
<span class="screenDescription">
<fmt:message key="label.recapScreen.description" /> <br/>
</span>
<span class="screenExplanations">
<fmt:message key="label.recapScreen.explanations" /> <br/>
</span>
<%
boolean connectOK = ctx.isConnectRegistrationDone();
%>
<%if (connectOK) {
   Map<String,String> connectMap = ctx.getConnectMap();%>
   <h2> <fmt:message key="label.connectFinish.ok" /> </h2>
   <div id="CLID"><%=connectMap.get("CLID")%></div><br/><br/>
<%}%>

  <table>
  <%Map<String,String> changedParams = collector.getChangedParameters();
  Map<String, String> sortedParams = new TreeMap<String, String>(collector.getConfigurationParams());
  for (String pName : sortedParams.keySet()) {
      String label = "label."+pName;%>
    <tr>
      <%if (changedParams.containsKey(pName) ||
              pName.startsWith("nuxeo.db") && !"default".equals(collector.getConfigurationParam(pName))) {%>
      <td style="font-weight: bold;">
      <%} else {%>
      <td>
      <%}%>
      <fmt:message key="<%=label%>"/></td>
      <%if (changedParams.containsKey(pName) ||
              pName.startsWith("nuxeo.db") && !"default".equals(collector.getConfigurationParam(pName))) {%>
      <td style="font-weight: bold;">
      <%} else {%>
      <td>
      <%}%>
      <%=collector.getConfigurationParam(pName)%></td>
    </tr>
  <%} %>
  </table>

 <div class="buttonContainer">
<%if (currentPage.prev()!=null) { %>
 <input type="button" id="btnPrev" class="glossyButton" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
<%}%>
 <input type="submit" id="btnNext" class="glossyButton" value="<fmt:message key="label.action.finish"/>"/>
</div>

</form>

<%@ include file="includes/footer.jsp" %>