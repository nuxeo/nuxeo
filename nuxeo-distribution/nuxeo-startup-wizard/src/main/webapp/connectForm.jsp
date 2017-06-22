<%@ include file="includes/header.jsp" %>
<%@ page import="org.nuxeo.wizard.helpers.ConnectRegistrationHelper" %>
<%
  String popupUrl = (String) request.getAttribute("popupUrl");

  boolean showRegistrationForm = !ctx.isConnectRegistrationDone();
  if (ConnectRegistrationHelper.isConnectRegistrationFileAlreadyPresent(ctx)) {
    showRegistrationForm = false;
  }
%>

<% if (showRegistrationForm) { %>
<script>
  function showFallback() {
    $('#fallback').css('display', 'block');
  }

  function openConnectRegister() {
    // Warning, it will only work when using a Same Origin popup's url
    var w = window.open("<%=popupUrl%>", 'Nuxeo Online Services', 'width=650,height=500');
    if (w === undefined) {
      showFallback();
    }

    // Keep a timed fallback in case Connect is not reachable
    setTimeout(showFallback, 120 * 1000);
  }

  $(openConnectRegister);
</script>
<h1><fmt:message key="label.connectFinish"/></h1>
<div class="formPadding">
  <p><fmt:message key="label.connectForm.popup.description"/></p>
  <p><a href="#" onclick="openConnectRegister()"><fmt:message key="label.connectForm.popup.manual"/></a></p>
</div>

<div style="display:none" id="fallback" class="formPadding">

  <p><fmt:message key="label.connectForm.loadError1"/></p>
  <p><fmt:message key="label.connectForm.loadError2"/></p>
  <input type="button" id="btnNext" class="glossyButton" value="<fmt:message key="label.action.next"/>"
         onclick="navigateTo('<%=currentPage.next().next().getAction()%>');"/>

</div>

<% } else { %>

<h1><fmt:message key="label.connectFinish.ok"/></h1>
<div class="formPadding" id="connectRegistered">
  <fmt:message key="label.connectFinish.ok.details"/>
  <%@ include file="includes/prevnext.jsp" %>
  <script>
    $('#btnNext').bind('click', function () {
      navigateTo('<%=currentPage.next().getAction()%>')
    });
  </script>
</div>

<%} %>

<%@ include file="includes/footer.jsp" %>
