<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Nuxeo Enterprise Platform, svn $Revision: 22925 $ -->
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ page language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<html>

<fmt:setBundle basename="messages" var="messages"/>

<head>
<title>Nuxeo Enterprise Platform 5.2</title>
<link rel="icon" type="image/png" href="/nuxeo/icons/favicon.png" />
<style type="text/css">
<!--
      body {
  font: normal 10px Verdana, sans-serif;
  background: url(/nuxeo/img/theme5_2/login5_2/login_black_background.jpg) 0 0 repeat-x #393939;
  color: white;

}

table.loginForm {
  border-spacing:3px;
  padding:3px;
  }

H1 {
       color:#0080ff;
       font:bold 14px Verdana, sans-serif;
       padding:0;
       margin:0 0 10px 0;
}

H2 {
       color:#999;
       font:bold 10px Verdana, sans-serif;
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

.login_label {
       font:bold 10px Verdana, sans-serif;
       text-align: right;
       color: #454545;
       margin:0 4px 0 0;
}

.login_input {
       border:1px inset #454545;
       background: white;
       padding:3px;
       color: #454545;
       margin:0 5px 5px 0px;
       font:normal 10px Verdana, sans-serif;
}

/* this class is a repeat because defined in nxthemes-setup.xml but
nxthemes css is not used in login.jsp */
.login_button {
       cursor:pointer;
       color: #454545;
       font-size: 10px;
font-weight:bold;
       background: url(/nuxeo/img/button_1.gif) 0 0 repeat-x #e3e6ea;
       border-style: solid;
       border-width: 1px;
       border-color: #ccc #666 #666 #ccc;
       padding: 2px 5px 2px 5px;
       margin: 2px;
}

.login_button:hover {
       color: #fff;
       font-size: 10px;
       background: url(/nuxeo/img/button_2.gif) 0 0 repeat-x #3f89ef;
       border-style: solid;
       border-width: 1px;
       border-color: #0099ff #0066cc #0066cc #0099ff;
       padding: 2px 5px 2px 5px;
       margin: 2px;
}

.formTitle {
       margin:0 0 20px 0;
       text-align:center;
       color:#4a4a4a;
       font-size:14px;
}

.loginLegal {
       color: #999;
       font-size: 9px;
       padding: 0;
       margin: 0 0 0 0;
}

.buttonsBar {
  margin:15px 0 0 0;
  padding-top:0px
  line-height:normal;
  width:138px;
  }

.buttonsBar ul {
  margin:0;
  padding:0px 0 0 0;
  list-style:none;
  }

.buttonsBar li {
  float:left;
  background:url(/nuxeo/img/theme5_2/login5_2/button_normal.gif) no-repeat left top;
  margin:5px 0 0 0;
  padding:10px 0 0 0;
  }

  .buttonsBar li:hover {
  float:left;
  background:url(/nuxeo/img/theme5_2/login5_2/button_hover.gif) no-repeat left top;
  }

.buttonsBar a {
  float:left;
  width:138px;
  text-decoration:none;
  color:#FFF;
  font:normal 9px Verdana, sans-serif;
  text-align:center;
  vertical-align:middle;
  padding-top:0px;
  padding-bottom:10px;
  }

.extensionsBar {
  margin:0;
  line-height:normal;
  width:138px;

  }
.extensionsBar ul {
  margin:5px 0 0 0;
  padding:0px 0 0 0;
  list-style:none;


  }

.extensionsBar li {
  float:left;
  margin:0px 0 0 0;
  padding:5px 0 0 0;
  }

.extensionsBar li.ie {
  background:url(/nuxeo/img/theme5_2/login5_2/ie_normal.gif) no-repeat left top;
  }

  .extensionsBar li.ie:hover {
  background:url(/nuxeo/img/theme5_2/login5_2/ie_hover.gif) no-repeat left top;
  }

.extensionsBar li.ff {
  background:url(/nuxeo/img/theme5_2/login5_2/ff_normal.gif) no-repeat left top;
}

  .extensionsBar li.ff:hover {
  background:url(/nuxeo/img/theme5_2/login5_2/ff_hover.gif) no-repeat left top;
  }

  .extensionsBar li.office {
  background:url(/nuxeo/img/theme5_2/login5_2/office_normal.gif) no-repeat left top;
  }

  .extensionsBar li.office:hover {
  background:url(/nuxeo/img/theme5_2/login5_2/office_hover.gif) no-repeat left top;
  }

.extensionsBar a {
  float:left;
  width:138px;
  text-decoration:none;
  color:#FFF;
  font:normal 9px Verdana, sans-serif;
  text-align:left;
  vertical-align:middle;
  padding-top:7px;
  padding-bottom:14px;
  padding-left:48px;

  }

.errorMessage {
  color:#000;
    font:bold 10px Verdana, sans-serif;
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


<form method="post" action="nxstartup.faces"><!-- To prevent caching -->
<%
    response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", -1); // Prevents caching at the proxy server
%> <!-- ;jsessionid=<%=request.getSession().getId()%> --> <!-- ImageReady Slices (login_cutted.psd) -->
<table align="center" width="650" height="500" border="0"
        cellpadding="0" cellspacing="0">
        <tr>
                <td colspan="6"><img src="/nuxeo/img/theme5_2/login5_2/login_01.jpg"
                        width="650" height="127" alt=""></td>
        </tr>
        <tr>
                <td rowspan="5"><img src="/nuxeo/img/theme5_2/login5_2/login_02.jpg"
                        width="41" height="372" alt=""></td>
                <td rowspan="3" background="/nuxeo/img/theme5_2/login5_2/login_03.jpg"
                        width="138" height="314" valign="middle">
                <table cellspacing="0" cellpadding="0" border="0" align="center"
                        width="138">
                        <tr>
                                <td align="left" valign="top">

                <div class="extensionsBar">
                <h2>
                  <fmt:message bundle="${messages}" key="label.login.improveYourExperience" />
                </h2>
                <ul>
                    <li class="ie"><a href="http://download.nuxeo.org/desktop-integration/drag-drop/msie/">
                      <fmt:message bundle="${messages}" key="label.login.IEExtension" />
                    </a></li>
                    <li class="ff"><a href="http://download.nuxeo.org/desktop-integration/drag-drop/firefox/Nuxeo5-FirefoxExtension.xpi">
                      <fmt:message bundle="${messages}" key="label.login.firefoxExtension" />
                    </a></li>
                    <li class="office"><a href="http://download.nuxeo.org/desktop-integration/live-edit/">
                      <fmt:message bundle="${messages}" key="label.login.liveEdit" />
                    </a></li>
                </ul>
                </div>


                <div class="buttonsBar">
                <h2>
                  <fmt:message bundle="${messages}" key="label.login.goFurther" />
                </h2>
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
                </ul>
                </div>
                </td>
            </tr>
        </table>

        </td>
        <td colspan="4"><img src="/nuxeo/img/theme5_2/login5_2/login_04.jpg"
            width="471" height="48" alt=""></td>
    </tr>
    <tr>
        <td rowspan="2"><img src="/nuxeo/img/theme5_2/login5_2/login_05.jpg"
            width="87" height="266" alt=""></td>
        <td background="/nuxeo/img/theme5_2/login5_2/login_06.jpg" width="303"
            height="174">

        <table cellpadding="0" border="0" align="center" class="loginForm">
            <tr>
                <td colspan="2" align="center">
                <h2 class="formTitle"><fmt:message bundle="${messages}" key="label.login.welcomeToNuxeoEnterprise" /></h2>
                </td>
            </tr>
            <tr>
                <td align="right"><label class="login_label" for="username">
                  <fmt:message bundle="${messages}" key="label.login.username" />
                </label></td>
                <td><input class="login_input" type="text"
                    name="user_name" id="username" size="22"></td>
            </tr>
            <tr>
                <td align="right"><label class="login_label" for="password">
                  <fmt:message bundle="${messages}" key="label.login.password" />
                </label></td>
                <td><input class="login_input" type="password"
                    name="user_password" id="password" size="22"></td>
            </tr>

            <tr>
                <td>&nbsp;</td>
                <% // label.login.logIn %>
                <td><input type="hidden" name="form_submitted_marker"
                    id="form_submitted_marker">
                    <input class="login_button" type="submit" name="Submit"
                    value="<fmt:message bundle="${messages}" key="label.login.logIn" />"></td>
            </tr>

        </table>

        </td>
        <td colspan="2" rowspan="2"><img
            src="/nuxeo/img/theme5_2/login5_2/login_07.jpg" width="81" height="266" alt=""></td>
    </tr>
    <tr>
        <td background="/nuxeo/img/theme5_2/login5_2/login_08.jpg" width="303"
            height="92">
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
        </td>
    </tr>
    <tr>
        <td colspan="4" background="/nuxeo/img/theme5_2/login5_2/login_09.jpg"
            width="558" height="43" align="center">
        <p class="loginLegal">
            <fmt:message bundle="${messages}" key="label.login.copyright" />
        </p>
        </td>
        <td rowspan="2"><img src="/nuxeo/img/theme5_2/login5_2/login_10.jpg"
            width="51" height="58" alt=""></td>
    </tr>
    <tr>
        <td colspan="4"><img src="/nuxeo/img/theme5_2/login5_2/login_11.jpg"
            width="558" height="15" alt=""></td>
    </tr>
    <tr>
        <td colspan="6"><img src="/nuxeo/img/theme5_2/login5_2/login_reflect.jpg"
            alt=""></td>
    </tr>
    <tr>
        <td><img src="/nuxeo/img/theme5_2/login5_2/spacer.gif" width="41"
            height="1" alt=""></td>
        <td><img src="/nuxeo/img/theme5_2/login5_2/spacer.gif" width="138"
            height="1" alt=""></td>
        <td><img src="/nuxeo/img/theme5_2/login5_2/spacer.gif" width="87"
            height="1" alt=""></td>
        <td><img src="/nuxeo/img/theme5_2/login5_2/spacer.gif" width="303"
            height="1" alt=""></td>
        <td><img src="/nuxeo/img/theme5_2/login5_2/spacer.gif" width="30"
            height="1" alt=""></td>
        <td><img src="/nuxeo/img/theme5_2/login5_2/spacer.gif" width="51"
            height="1" alt=""></td>
    </tr>
</table>
<!-- End ImageReady Slices --></form>

<!--   Current User = <%=request.getRemoteUser()%> -->
</body>
</html>
