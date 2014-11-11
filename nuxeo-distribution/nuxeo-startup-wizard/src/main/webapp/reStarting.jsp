<%@page import="org.nuxeo.wizard.helpers.ServerController"%>
<%@ include file="includes/header.jsp"%>

<div id="loading"><fmt:message key="label.restart.wait" /><br/><br/>
<img src="<%=contextPath%>/images/restart_waiter.gif" /></div>

<%@ include file="includes/footer.jsp"%>

<%
    // do the actual restart once we have displayed the waiting page
    ServerController.restart(ctx);
%>

<script type="text/javascript">
<!--
// be sure Ajax Requests will timeout quickly
$.ajaxSetup( {
  timeout: 8000
} );

// start polling after 15s to be sure the server begun the restart
setTimeout(startPolling, 15000);

// polls until login page is available again
function startPolling() {
  var intId = setInterval(function isNuxeoReady() {
    var sc = $("#reloadPage");
    if (sc) sc.remove();
    sc = $("<script></script>");
    sc.attr("id","reloadPage");
    sc.attr("src","<%=ServerController.getServerURL()%>/runningstatus?info=reload");
    $("body").append(sc);
  }, 10000);
}

function reload() {
  window.location.href='<%=ServerController.getServerURL()%>/';
}
-->
</script>
