<%@ include file="includes/header.jsp"%>
<%
    String welcomeLabel = "label.welcome."
            + collector.getDistributionName();
%>
<h1><fmt:message key="<%=welcomeLabel%>" /></h1>
<form id="wizardform" action="<%=contextPath%>/<%=currentPage.getAction()%>" method="POST">
<div class="formPadding">
<p><fmt:message key="label.welcome.p1" /></p>
<p><fmt:message key="label.welcome.p2" /></p>
<p><b><fmt:message key="label.welcome.p3" /></b></p>
<p><fmt:message key="label.welcome.p4" />
<ul>
    <li><a href="https://doc.nuxeo.com/x/EIAV" target="doc"> <fmt:message
        key="label.welcome.p4a" /></a></li>
    <li><a href="https://doc.nuxeo.com/x/lYFH" target="doc"> <fmt:message
        key="label.welcome.p4b" /></a></li>
</ul>
</p>

<input type="hidden" name="baseUrl" id="baseUrl" value=""/>

</div>
<center>
 <input type="submit" class="glossyButton" id="btnNext" value="<fmt:message key="label.action.next"/>"/>
</center>
<script>
  $(document).ready(function(){
    $("#baseUrl").attr("value",window.location.href);
  });
</script>
</form>
<%@ include file="includes/footer.jsp"%>