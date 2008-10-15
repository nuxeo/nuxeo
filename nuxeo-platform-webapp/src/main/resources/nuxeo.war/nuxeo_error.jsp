<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page language="java" %>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>
<fmt:setBundle basename="messages" var="messages"/>
<head>
  <title>Nuxeo Error Page</title>
  <style type="text/css">
<!--
body {
  background: url( /nuxeo/img/fond_error.gif ) 0 0 repeat-x #ffffff;
  margin: 0px;
}

H1 {
  color: #0080ff;
  font: bold 20px Verdana, sans-serif;
  margin: 70px 0 40px 0;
}

H2 {
  color: #505050;
  font: bold 12px Verdana, sans-serif;
  margin: 15px 0 15px 0;
  padding: 5px;
  background: #EBF5FF;
  border: 1px solid #3299ff;
}

a {
  font-family: Verdana, sans-serif;
  color: #595959;
  font-style: sans-serif;
  font-size: 10pt;
}

a:hover {
  color: #404040;
}

.links {
  font-family: Verdana, sans-serif;
  color: #7B7B7B;
  font-style: sans-serif;
  font-size: 10pt;
  margin: 40px 0 0 0;
}

.logo {
  margin: 0 70px 0 0;
}

.stacktrace {
  padding: 0 5px 0 20px;
  background: url( /nuxeo/icons/page_white_text.png ) no-repeat scroll 0%;
  margin: 10px 0 0 0;
}

.back {
  padding: 0 5px 0 20px;
  background: url( /nuxeo/icons/back.png ) no-repeat scroll 0%;
  margin: 10px 0;
}

.change {
  padding: 0 5px 0 20px;
  background: url( /nuxeo/icons/user_go.png ) no-repeat scroll 0%;
}

#stackTrace {
  border: 1px solid #999999;
  padding: 3px;
  margin: 15px 0;
  width: 700px;
  height: 700px;
  overflow: auto;
}

-->
  </style>
  <script language="javascript" type="text/javascript">
    function toggleError(id) {
      var style = document.getElementById(id).style;
      if ("block" == style.display) {
        style.display = "none";
        document.getElementById(id + "Off").style.display = "inline";
        document.getElementById(id + "On").style.display = "none";
      } else {
        style.display = "block";
        document.getElementById(id + "Off").style.display = "none";
        document.getElementById(id + "On").style.display = "inline";
      }
    }
  </script>
</head>
<body>
<%
  String user_message = (String) request.getAttribute("user_message");
  String exception_message = (String) request.getAttribute("exception_message");
  String stackTrace = (String) request.getAttribute("stackTrace");
  String context = request.getContextPath();
  Boolean securityError = (Boolean) request.getAttribute("securityError");

  String pageTitle="An error occured";
  if ((securityError!=null) && (securityError.booleanValue()==true))
  {
    pageTitle = "You don't have the neccessary permission to do the requested action";
  }
  boolean isAnonymous = AnonymousAuthenticator.isAnonymousRequest(request);

%>

<table border="0" width="75%" cellpadding="0" cellspacing="0" align="center">
  <tr>
    <td width="280" align="right" valign="top">
      <div class="logo">
        <img src="/nuxeo/img/logo_error.gif" alt="">
      </div>
    </td>
    <td>

      <h1><%=pageTitle%></h1>

  <% if (!isAnonymous) { %>
      <h2>
      <fmt:message bundle="${messages}" key="${user_message}" />
      </h2>


      <div class="links">
        <div class="back"><a href="javascript:history.back();">back</a>
        </div>
        <div class="change"><a href="<%=context%>/logout">change username</a>
        </div>
        <div class="stacktrace">
          <a href="#"
             onclick="javascript:toggleError('stackTrace'); return false;">show
            stacktrace</a>
        </div>
        <div id="stackTrace" style="display: none;">
          <h2><%=exception_message %>
          </h2>
          <inputTextarea rows="20" cols="100" readonly="true">
            <%=stackTrace%>
          </inputTextarea>
        </div>
      </div>
      <%} else { %>
      <h2> You must be authenticated to perform this operation </h2>
      <div class="change"><a href="<%=context%>/logout">Login</a>

      <%} %>

    </td>
  </tr>
</table>

</body>
</html>