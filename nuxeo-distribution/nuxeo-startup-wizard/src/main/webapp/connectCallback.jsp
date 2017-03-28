<html>
<script>
  window.opener.top.location.href = '<%=request.getAttribute("targetUrl")%>';
  window.close();
</script>
</html>