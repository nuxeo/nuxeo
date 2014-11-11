<%@ include file="includes/header.jsp"%>
<%
    String welcomeLabel = "label.welcome."
            + collector.getDistributionName();
%>
<h1><fmt:message key="<%=welcomeLabel%>" /></h1>

<div class="formPadding">
<p><fmt:message key="label.welcome.p1" /></p>
<p><fmt:message key="label.welcome.p2" /></p>
<p><b><fmt:message key="label.welcome.p3" /></b></p>
<p><fmt:message key="label.welcome.p4" />
<ul>
    <li><a href="https://doc.nuxeo.com/x/lYFH" target="doc"> <fmt:message
        key="label.welcome.p4a" /></a></li>
    <li><a href="https://doc.nuxeo.com/x/EIAV" target="doc"> <fmt:message
        key="label.welcome.p4b" /></a></li>
</ul>
</p>


</div>
<center><input type="button" class="glossyButton"
    value="<fmt:message key="label.action.next"/>"
    onclick="navigateTo('<%=currentPage.next().getAction()%>');" /></center>

<%@ include file="includes/footer.jsp"%>