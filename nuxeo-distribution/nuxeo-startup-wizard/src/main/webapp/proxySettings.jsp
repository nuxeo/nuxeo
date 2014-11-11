<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.proxySettings" /></h1>

<%
String proxyType = collector.getConfigurationParam("org.nuxeo.connect.proxy.type");
String proxySettingStyle="";
String proxyLoginStyle="display:none";
if (proxyType==null || proxyType.equals("")) {
    proxyType="none";
}
if (proxyType.equals("none")) {
    proxySettingStyle="display:none";
    proxyLoginStyle="display:none";
} else if (proxyType.equals("anonymous")) {
    proxySettingStyle="display:block";
    proxyLoginStyle="display:none";
} else if (proxyType.equals("authenticated")) {
    proxySettingStyle="display:block";
    proxyLoginStyle="display:block";
}
%>
<script language="javascript">
function updateProxySettings() {
  var value = document.getElementById("proxyTypeSelector").value;
  if (value=='none') {
    $('#proxySettings').css('display','none');
    $('#proxyLogin').css('display','none');
  } else if (value=='anonymous') {
    $('#proxySettings').css('display','block');
    $('#proxyLogin').css('display','none');
  } else if (value=='authenticated') {
    $('#proxySettings').css('display','block');
    $('#proxyLogin').css('display','block');
  } else {
    alert("not found *" + value + "*");
  }
}
</script>

<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.proxySettings.description" /> <br/>
</span>

<span class="screenExplanations">
<fmt:message key="label.proxySettings.explanations" /> <br/>
</span>

<%@ include file="includes/feedback.jsp" %>
   <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.org.nuxeo.connect.proxy.type"/></td>
      <td>
  <select id="proxyTypeSelector" name="org.nuxeo.connect.proxy.type" onchange="updateProxySettings()">
     <option
     <%if ("none".equals(proxyType) || "".equals(proxyType) ){%>
     selected
     <%} %>
     value="none"><fmt:message key="label.proxySettings.none" /></option>
     <option
     <%if ("anonymous".equals(proxyType) ){%>
     selected
     <%} %>
     value="anonymous"><fmt:message key="label.proxySettings.anonymous" /></option>
     <option
     <%if ("authenticated".equals(proxyType) ){%>
     selected
     <%} %>
     value="authenticated"><fmt:message key="label.proxySettings.authenticated" /></option>
  </select>
</td>
</tr></table>

  <div id="proxySettings" style="<%=proxySettingStyle%>">
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.org.nuxeo.connect.proxy.host"/></td>
      <td><input type="text" name="org.nuxeo.connect.proxy.host" value="<%=collector.getConfigurationParam("org.nuxeo.connect.proxy.host") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.org.nuxeo.connect.proxy.port"/></td>
      <td><input type="text" name="org.nuxeo.connect.proxy.port" value="<%=collector.getConfigurationParam("org.nuxeo.connect.proxy.port") %>" size="4" /></td>
    </tr>
  </table>
  </div>

  <div id="proxyLogin" style="<%=proxyLoginStyle%>">
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.org.nuxeo.connect.proxy.login"/></td>
      <td><input type="text" name="org.nuxeo.connect.proxy.login" value="<%=collector.getConfigurationParam("org.nuxeo.connect.proxy.login") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.org.nuxeo.connect.proxy.password"/></td>
      <td><input type="password" name="org.nuxeo.connect.proxy.password" value="<%=collector.getConfigurationParam("org.nuxeo.connect.proxy.password") %>"/></td>
    </tr>
  </table>
  </div>

<%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>