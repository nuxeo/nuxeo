<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform, svn $Revision: 22925 $ -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ page import="org.nuxeo.runtime.api.Framework"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
String productName = Framework.getProperty("org.nuxeo.ecm.product.name");
String productVersion = Framework.getProperty("org.nuxeo.ecm.product.version");
%>
<html>

<fmt:setBundle basename="messages" var="messages"/>

<head>
<title><%=productName%></title>
<link rel="icon" type="image/png" href="/nuxeo/icons/favicon.png" />
<style type="text/css">
<!--
 body {
  font: normal 11px "Lucida Grande", sans-serif;
  background: url(/nuxeo/img/theme_galaxy/boston4.jpg) 0 0 no-repeat #000;
  color: #343434;
  }

.topBar {
  background:#212325 url(/nuxeo/img/theme_galaxy/small_gray_bar.png) repeat-x scroll left top;
  width:100%;
  height:36px;
  border:0;
  }

.topBar img {
  margin-left:70px;
  }

table.loginForm {
  border-spacing:3px;
  padding:3px;
  }

.leftColumn {
  width:300px;
  }

H1 {
  color:#343434;
  font:bold 14px "Lucida Grande", sans-serif;
  padding:0;
  margin:2px 0 15px 0;
  border-bottom:1px dotted #8B8B8B;
  }

H2 {
  color:#999;
  font:bold 10px "Lucida Grande", sans-serif;
  padding:0;
  margin:0 0 0 0;
  }

.extensionButtons {
  padding:0;
  margin:0 0 0 0;
  }

.linkButtons {
  padding:0;
  margin:0 0 0 0;
  }

.login {
  background:#fff;
  opacity:0.8;
  filter : alpha(opacity=80);
  border: 1px solid #4E9AE1;
  padding:20px 75px 5px 70px;
  width:250px;
  }

.login_label {
  font:bold 10px "Lucida Grande", sans-serif;
  text-align: right;
  color: #454545;
  margin:0 4px 0 0;
  width:70px;
  }

.login_input {
  border:1px inset #454545;
  background: white;
  padding:3px;
  color: #454545;
  margin:0 10px 5px 0px;
  font:normal 10px "Lucida Grande", sans-serif;
  }

/* this class is a repeat because defined in nxthemes-setup.xml but
nxthemes css is not used in login.jsp */
.login_button {
  cursor:pointer;
  color: #454545;
  font-size: 10px;
  background: #CECFD1 url(/nuxeo/img/theme_galaxy/buttons.png) repeat-x scroll left top;
  border:1px solid #BFC5CB;
  padding: 2px 5px 2px 5px;
  margin: 5px 10px 10px 0;
  }

.login_button:hover {
  border:1px solid #92999E;
  color:#000000;
  }

.formTitle {
  margin:0 0 20px 0;
  text-align:center;
  color:#4a4a4a;
  font-size:14px;
  }

.footer {
  color: #d6d6d6;
  font-size: 9px;
  }

.loginLegal {
  padding: 0;
  margin: 0 0 10px 0;
  }

.version {
  padding-right:50px;
  }


.labelCorp {
  margin:0;
  width:400px;
  padding-top:0px;
  }

.labelCorp ul{
  margin:0;
  padding:0 42px 0 0;
  }

.labelCorp li {
  margin:0;
  padding:0px 8px;
  list-style:none;
  float:right;
  }


.labelCorp a {
  text-decoration:none;
  color:#9a9a9a;
  font:normal 9px "Lucida Grande", sans-serif;
  padding-top:0px;
  }

.labelCorp a:hover {
  text-decoration:underline;
  }


.block_container {
  margin-right:50px;
  border:none;
  height:500px;
  width:350px;
  overflow:auto;
  background-color:#ffffff;
  opacity:0.8;
  filter : alpha(opacity=80);
  }

.errorMessage {
  color:#000;
  font:bold 10px "Lucida Grande", sans-serif;
  border:1px solid #666;
  background: url(/nuxeo/img/warning.gif) 2px 3px no-repeat #FFCC33;
  margin-bottom:12px;
  display:block;
  padding:5px 5px 5px 23px;
  text-align: center;
  }
-->

</style>
</head>

<body style="margin:0;text-align:center;">

  <table cellspacing="0" cellpadding="0" border="0" width="100%" height="100%">
    <tbody>
      <tr class="topBar">
        <td>
          <img width="107" height="36" alt="Nuxeo EP" src="/nuxeo/img/theme_galaxy/nuxeo_5.2_logo.png"/>
       </td>
       <td align="right" class="leftColumn">
       <div class="labelCorp">
       <ul>
                    <li><a href="http://www.nuxeo.com/en">
                      <fmt:message bundle="${messages}" key="label.login.visitNuxeoCom" />
                    </a></li>
                    <li><a href="http://www.nuxeo.com/en/services/support">
                      <fmt:message bundle="${messages}" key="label.login.getSupport" />
                    </a></li>
                    <li><a href="http://www.nuxeo.org/sections/community/">
                      <fmt:message bundle="${messages}" key="label.login.joinTheCommunity" />
                    </a></li>
                    <li><a href="http://doc.nuxeo.org/xwiki/bin/view/Main/QuickStart-5.2">
                      <fmt:message bundle="${messages}" key="label.login.quickStart" />
                    </a></li>
                </ul>
                <div style="clear:both;" />
          </div>
       </td>
      </tr>
      <tr>
        <td align="center">
             <form method="post" action="nxstartup.faces"><!-- To prevent caching -->
<%
    response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
%> <!-- ;jsessionid=<%=request.getSession().getId()%> --> <!-- ImageReady Slices (login_cutted.psd) -->
      <!--
      <h2 class="formTitle"><fmt:message bundle="${messages}" key="label.login.welcomeToNuxeoEnterprise" /></h2>
      -->
        <div class="login">
          <table>
           <tr>
             <td class="login_label">
              <label for="username">
                  <fmt:message bundle="${messages}" key="label.login.username" />
                </label>
                </td>
                <td>
                <input class="login_input" type="text"
                    name="user_name" id="username" size="22">
            </td>
            </tr>
            <tr>
            <td class="login_label">
              <label for="password">
                  <fmt:message bundle="${messages}" key="label.login.password" />
                </label>
                </td>
                <td>
                <input class="login_input" type="password"
                    name="user_password" id="password" size="22">
                 </td>
                 </tr>
                 <tr>
                 <td></td>
                 <td>
                                    <% // label.login.logIn %>
                <input type="hidden" name="requestedUrl"
                    id="requestedUrl" value="${param.requestedUrl}">
                <input type="hidden" name="form_submitted_marker"
                    id="form_submitted_marker">
                    <input class="login_button" type="submit" name="Submit"
                    value="<fmt:message bundle="${messages}" key="label.login.logIn" />">

              </td>
            </tr>
            <tr>
              <td></td>
              <td>
          <c:if test="${param.loginFailed}">
            <div class="errorMessage">
                  <fmt:message bundle="${messages}" key="label.login.invalidUsernameOrPassword" />
            </div>
          </c:if>
          <c:if test="${param.loginMissing}">
            <div class="errorMessage">
                  <fmt:message bundle="${messages}" key="label.login.missingUsername" />
            </div>
          </c:if>
          <c:if test="${param.securityError}">
            <div class="errorMessage">
                  <fmt:message bundle="${messages}" key="label.login.securityError" />
            </div>
          </c:if>
      </td>
    </tr>
    </table>
      </form>
      </td>
      <td class="news_container" align="right" valign="center">
        <iframe class="block_container" style="display:none"
          onload="javascript:this.style.display='block';"
          src="http://www.nuxeo.com/var/storage/nuxeo_dm/news.html"></iframe>
      </td>
    </tr>
      <tr class="footer">
        <td align="center" valign="bottom">
         <div class="loginLegal">
            <fmt:message bundle="${messages}" key="label.login.copyright" />
        </div>
        </td>
        <td align="right" class="version" valign="bottom">
        <div class="loginLegal">


         <%=productName%>
         &nbsp;
         <%=productVersion%>

        </div>
        </td>
      </tr>
    </tbody>
  </table>
<!--   Current User = <%=request.getRemoteUser()%> -->
</body>
</html>

