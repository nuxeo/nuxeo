<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.connectFinish" /></h1>

<%
boolean registrationOK = ctx.isConnectRegistrationDone();
Map<String,String> connectMap = ctx.getConnectMap();
%>

<%@ include file="includes/form-start.jsp" %>

<%if (registrationOK) { %>

   <h2> <fmt:message key="label.connectFinish.ok" /> </h2>
   <div><%=connectMap.get("CLID")%></div>

   <%@ include file="includes/prevnext.jsp" %>
<%} else { %>

  <h2> <fmt:message key="label.connectFinish.ko" /> </h2>
  <ul>
   <li><fmt:message key="label.connectFinish.ko.bad1" /></li>
   <li><fmt:message key="label.connectFinish.ko.bad2" /></li>
  </ul>
  <br/>

 <input type="button" class="glossyButton" value="<fmt:message key="label.action.retry"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
 <input type="submit" class="glossyButton" value="<fmt:message key="label.action.skip"/>"/>

 </form>

<%}%>




<%@ include file="includes/footer.jsp" %>