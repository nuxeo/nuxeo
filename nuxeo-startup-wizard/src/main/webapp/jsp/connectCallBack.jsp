<html>
<%
String action = request.getParameter("action");
String prev = request.getParameter("prev");
String next = request.getParameter("next");
String token = request.getParameter("ConnectRegistrationToken");
if (action==null || action.isEmpty()) {
    action="skip";
}
if (action.equals("register") && (token==null || token.isEmpty())) {
    action = "skip";
}

String url = request.getContextPath() + "/";
if (action.equals("skip")) {
    url += next + "?skip=true";
} else if (action.equals("register")) {
    url += next + "?ConnectRegistrationToken=" + token;
} else if (action.equals("prev")) {
    url += prev;
}

%>
<script>
window.top.location.href = '<%=url%>';
</script>
</html>