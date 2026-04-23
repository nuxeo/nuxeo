<!DOCTYPE html>
  <%@ page contentType="text/html; charset=UTF-8"%>

  <%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
  <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

    <%
    String context = request.getContextPath();
    %>

  <html>
  <head>
  <meta charset="utf-8">

  <!-- Enable IE Standards mode -->
  <meta http-equiv="x-ua-compatible" content="ie=edge">

  <title></title>
  <c:choose>
    <c:when test="${not empty favIconUrl}">
      <link rel="icon" type="image/x-icon" href="${fn:escapeXml(favIconUrl)}" />
      <link rel="shortcut icon" type="image/x-icon" href="${fn:escapeXml(favIconUrl)}" />
    </c:when>
    <c:otherwise>
      <link rel="icon" type="image/png" href="<%=context%>/icons/favicon.png" />
      <link rel="shortcut icon" type="image/x-icon" href="<%=context%>/icons/favicon.ico" />
    </c:otherwise>
  </c:choose>
  <meta name="description" content="">
  <meta name="viewport"
  content="width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no">


  <style type="text/css">
  body {
  margin: 0;
  padding: 0;
  overflow:hidden;
  -ms-content-zooming: none;
  }
  #office_frame {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  margin: 0;
  border: none;
  display: block;
  }
  </style>
  </head>
<body>

  <form id="office_form" name="office_form" target="office_frame"
  action="${fn:escapeXml(formURL)}" method="post">
  <input name="access_token" value="${fn:escapeXml(accessToken)}" type="hidden"/>
  <input name="access_token_ttl" value="${fn:escapeXml(accessTokenTTL)}" type="hidden"/>
  </form>

  <span id="frameholder"></span>

  <script type="text/javascript" src="<%=context%>/scripts/wopi.js"></script>

</body>
</html>
