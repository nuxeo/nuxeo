<%@ page session="false" %>
<%
  String serviceId = (String) request.getAttribute("serviceId");
  String token = (String) request.getAttribute("token");
  String service = null;
  if (serviceId.indexOf('?') == -1)
    service = serviceId + "?ticket=" + token;
  else
    service = serviceId + "&ticket=" + token;
  service =
    edu.yale.its.tp.cas.util.StringUtil.substituteAll(service, "\n", "");
  service =
    edu.yale.its.tp.cas.util.StringUtil.substituteAll(service, "\r", "");
  service =
    edu.yale.its.tp.cas.util.StringUtil.substituteAll(service, "\"", "");
%>
<html>
<head>
<title>Yale Central Authentication Service</title>
 <script>
  window.location.href="<%= service %>";
 </script>
</head>

<body bgcolor="#6699CC">
 <noscript>
  <p>Login successful.</p>
  <p>
   Click <a href="<%= service %>" />">here</a>
   to access the service you requested.
  </p>
 </noscript>
</body>

</html>
