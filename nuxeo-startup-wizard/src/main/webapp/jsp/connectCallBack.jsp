<html>
<script>
var url = '<%=request.getContextPath()%>/<%=request.getParameter("next")%>';
url += '?ConnectRegistrationToken=<%=request.getParameter("ConnectRegistrationToken")%>';
window.top.location.href = url;
</script>
</html>