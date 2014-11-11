
<%@page import="java.io.PrintWriter"%><html>

<h1>Error screen</h1>

<%
Exception e = (Exception)request.getAttribute("error");
%>
Error : <%=e.getClass().getSimpleName()%>

<pre>
<%
e.printStackTrace(response.getWriter());
%>
</pre>

</html>