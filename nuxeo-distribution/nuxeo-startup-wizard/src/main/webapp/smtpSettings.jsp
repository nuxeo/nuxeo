<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.smtpSettings" /></h1>

<%
String smtpAuthType = collector.getConfigurationParam("mail.smtp.auth");
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
      <td class="labelCell"><fmt:message key="label.mail.smtp.host"/></td>
      <td><input type="text" name="mail.smtp.host" value="<%=collector.getConfigurationParam("mail.smtp.host") %>"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.smtp.host.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.smtp.port"/></td>
      <td><input type="text" name="mail.smtp.port" value="<%=collector.getConfigurationParam("mail.smtp.port") %>" size="4" /></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.smtp.port.help"/></td>
    </tr>

    <tr>
      <td class="labelCell"><fmt:message key="label.mail.smtp.auth"/></td>
      <td>
  <select id="smtpAuthTypeSelector" name="mail.smtp.auth" onchange="updateSmtpSettings()">
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
  <td colspan="2" class="helpCell"><fmt:message key="label.mail.smtp.auth.help"/></td>
  </tr>
  </table>

  <div id="smtpAuthSettings" style="<%=smtpAuthSettingStyle%>">
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.smtp.username"/></td>
      <td><input type="text" name="mail.smtp.username" value="<%=collector.getConfigurationParam("mail.smtp.username") %>"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.smtp.username.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.mail.smtp.password"/></td>
      <td><input type="password" name="mail.smtp.password" value="<%=collector.getConfigurationParam("mail.smtp.password") %>"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.mail.smtp.password.help"/></td>
    </tr>

  </table>
 </div>

  <%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>