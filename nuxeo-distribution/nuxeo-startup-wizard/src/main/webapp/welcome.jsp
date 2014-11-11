<%@ include file="includes/header.jsp"%>
<script src="<%=contextPath%>/scripts/browserInternetAccess.js"></script>
<script src="http://static.nuxeo.com/javascript/networkCheck.js?ts=<%=System.currentTimeMillis()%>"></script>

<h1><fmt:message key="label.welcome" /></h1>
<form id="wizardform" action="<%=contextPath%>/<%=currentPage.getAction()%>" method="POST">
<div class="formPadding">
<p><fmt:message key="label.welcome.p1" /></p>
<p><fmt:message key="label.welcome.p2" /></p>
<p><b><fmt:message key="label.welcome.p3" /></b></p>
<p><a href="https://doc.nuxeo.com/x/lYFH" target="doc"><fmt:message key="label.welcome.p4" /></a></p>

<input type="hidden" name="baseUrl" id="baseUrl" value=""/>
<input type="hidden" name="browserInternetAccess" id="browserInternetAccess" value=""/>
</div>
<div class="buttonContainer">
 <input type="submit" class="glossyButton" id="btnNext" disabled="true" value="<fmt:message key="label.action.start"/>"/>
</div>
<script>
  $(document).ready(function(){
    $("#baseUrl").attr("value",window.location.href);
    $("#browserInternetAccess").attr("value",hasBrowserInternetAccess());
    $("#btnNext").attr("disabled",false);
  });
</script>
</form>
<%@ include file="includes/footer.jsp"%>
