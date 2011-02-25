<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.connectFinish" /></h1>

<%
boolean registrationOK = ctx.isConnectRegistrationDone();
Map<String,String> connectMap = ctx.getConnectMap();
%>

<%@ include file="includes/form-start.jsp" %>

<%if (registrationOK) { %>

   <h2> <fmt:message key="label.connectFinish.ok" /> </h2>

   <div><fmt:message key="label.connectFinish.CLID" /> <%=connectMap.get("CLID")%></div>

   <p>
       <fmt:message key="label.connectFinish.usage" />
   </p>

   <p class="details">
       <fmt:message key="label.connectFinish.needValidate" />
   </p>

   </div>
 <center>
 <input type="button" class="glossyButton" value="<fmt:message key="label.action.prev"/>" onclick="navigateTo('<%=currentPage.prev().prev().getAction()%>');"/>
 <input type="submit" class="glossyButton" value="<fmt:message key="label.action.next"/>"/>
 </center>
 </form>
<%} else { %>

  <h2> <fmt:message key="label.connectFinish.ko" /> </h2>
  <ul>
   <li><fmt:message key="label.connectFinish.ko.bad1" /></li>
   <li><fmt:message key="label.connectFinish.ko.bad2" /></li>
  </ul>
  <br/>
  <p class="details">
       <fmt:message key="label.connectFinish.ko.free" />
   </p>
 </div>
 <center>
 <input type="button" class="glossyButton" value="<fmt:message key="label.action.retry"/>" onclick="navigateTo('<%=currentPage.prev().getAction()%>');"/>
 <input type="submit" class="glossyButton" value="<fmt:message key="label.action.skip"/>"/>
 </center>
 </form>

<%}%>




<%@ include file="includes/footer.jsp" %>