<%@ include file="includes/header.jsp" %>

<h1> <fmt:message key="label.welcome" /></h1>

<p><fmt:message key="label.welcome.p1" /></p>
<p><fmt:message key="label.welcome.p2" /></p>
<p><fmt:message key="label.welcome.p3" />
<ul>
  <li><a href="https://doc.nuxeo.com/x/lYFH" target="doc">
  <fmt:message key="label.welcome.p3a" /></a></li>
  <li><a href="https://doc.nuxeo.com/x/EIAV" target="doc">
  <fmt:message key="label.welcome.p3b" /></a></li>
</ul>
</p>

<br/>
<center>
<input type="button" class="glossyButton" value="<fmt:message key="label.action.next"/>" onclick="navigateTo('<%=currentPage.next().getAction()%>');"/>
</center>

<%@ include file="includes/footer.jsp" %>