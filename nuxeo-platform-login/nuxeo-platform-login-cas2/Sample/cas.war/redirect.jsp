<%@ page session="false" %>
<%
  String serviceId = (String) request.getAttribute("serviceId");
%>
<html>
<head>
<title>Yale Central Authentication Gateway</title>
 <script>
  window.location.href="<%= serviceId %>";
 </script>
</head>

<body bgcolor="#0044AA">
 <noscript>
  <p>
   Click <a href="<%= serviceId %>">here</a>
   to access the service you requested.
  </p>
 </noscript>
</body>

</html>
