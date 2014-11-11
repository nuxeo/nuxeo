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
var currentUrl = "<%=request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + contextPath%>";
var newUrl = "<%=ServerController.getServerURL()%>";
// url check disabled for now, see NXP-8780
// if (currentUrl == newUrl)
if (true)
    setTimeout(startDirectPolling, 15000);
  else
    setTimeout(startIndirectPolling, 15000);

// polls until login page is available again
function startIndirectPolling() {
  var intId = setInterval(function isNuxeoReady() {
    var sc = $("#reloadPage");
    if (sc) sc.remove();
    sc = $("<script></script>");
    sc.attr("id","reloadPage");
    sc.attr("src", newUrl + "/runningstatus?info=reload");
    $("body").append(sc);
  }, 10000);
}

function startDirectPolling() {
  var intId = setInterval(function isNuxeoReady() {
      $.get("<%=contextPath%>/login.jsp", function(data, textStatus) {
          window.location.href='<%=contextPath%>/';
      });
  }, 10000);
}

function reload() {
  window.location.href = newUrl;
}
-->
</script>
