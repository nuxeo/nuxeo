<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.smtpSettings" /></h1>

<%
String smtpAuthType = collector.getConfigurationParam("mail.transport.auth");
String smtpAuthSettingStyle="display:none";
if (smtpAuthType==null || smtpAuthType.equals("")) {
    smtpAuthType="false";
}
if ("true".equals(smtpAuthType)) {
    smtpAuthSettingStyle="display:block";
}
%>

<script language="javascript">
function updateSmtpSettings() {
  var value = document.getElementById("smtpAuthTypeSelector").value;
  if (value=='false') {
    $('#smtpAuthSettings').css('display','none');
  } else {
    $('#smtpAuthSettings').css('display','block');
  }
}
</script>

<%@ include file="includes/form-start.jsp" %>

<span class="screenDescription">
<fmt:message key="label.smtpSettings.description" /> <br/>
</span>

<span class="screenExplanations">
<fmt:message key="label.smtpSettings.explanations" /> <br/>
</span>


<%@ include file="includes/feedback.jsp" %>
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.transport.host"/></td>
      <td><input type="text" name="mail.transport.host" value="<%=collector.getConfigurationParam("mail.transport.host") %>"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.transport.host.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.transport.port"/></td>
      <td><input type="text" name="mail.transport.port" value="<%=collector.getConfigurationParam("mail.transport.port") %>" size="4" /></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.transport.port.help"/></td>
    </tr>

    <tr>
      <td class="labelCell"><fmt:message key="label.mail.transport.auth"/></td>
      <td>
  <select id="smtpAuthTypeSelector" name="mail.transport.auth" onchange="updateSmtpSettings()">
     <option
     <%if ("false".equals(smtpAuthType) ){%>
     selected
     <%} %>
     value="false"><fmt:message key="label.smtpSettings.noAuth" /></option>
     <option
     <%if ("true".equals(smtpAuthType) ){%>
     selected
     <%} %>
     value="true"><fmt:message key="label.smtpSettings.auth" /></option>
  </select>
  </td>
  </tr><tr>
  <td colspan="2" class="helpCell"><fmt:message key="label.mail.transport.auth.help"/></td>
  </tr>
  </table>

  <div id="smtpAuthSettings" style="<%=smtpAuthSettingStyle%>">
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.transport.user"/></td>
      <td><input type="text" name="mail.transport.user" value="<%=collector.getConfigurationParam("mail.transport.user") %>"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.transport.user.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.transport.password"/></td>
      <td><input type="password" name="mail.transport.password" value="<%=collector.getConfigurationParam("mail.transport.password") %>"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.transport.password.help"/></td>
    </tr>

  </table>
 </div>

  <%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>