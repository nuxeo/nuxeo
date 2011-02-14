<%@ include file="includes/header.jsp" %>

<h1> <fmt:message key="label.welcome" /></h1>

<p><fmt:message key="label.welcome.p1" /></p>
<p><fmt:message key="label.welcome.p2" /></p>
<p><fmt:message key="label.welcome.p3" />
<ul>
  <li><fmt:message key="label.welcome.p3a" /></li>
  <li><fmt:message key="label.welcome.p3b" /></li>
</ul>
</p>

<br/>
<center>
<input type="button" class="glossyButton" value="<fmt:message key="label.action.next"/>" onclick="navigateTo('<%=currentPage.next().getAction()%>');"/>
</center>

<%@ include file="includes/footer.jsp" %>