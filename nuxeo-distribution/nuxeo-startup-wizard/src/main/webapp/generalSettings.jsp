<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.generalSettings" /></h1>

<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.generalSettings.description" /> <br/>
</span>

<span class="screenExplanations">
<fmt:message key="label.generalSettings.explanations" /> <br/>
</span>

<%@ include file="includes/feedback.jsp" %>
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.bind.address"/></td>
      <td><input type="text" name="nuxeo.bind.address" value="<%=collector.getConfigurationParam("nuxeo.bind.address") %>" size="15"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.bind.address.help"/></td>
    </tr>

    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.log.dir"/></td>
      <td><input type="text" name="nuxeo.log.dir" value="<%=collector.getConfigurationParam("nuxeo.log.dir") %>" size="35"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.log.dir.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.data.dir"/></td>
      <td><input type="text" name="nuxeo.data.dir" value="<%=collector.getConfigurationParam("nuxeo.data.dir") %>" size="35"/></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.data.dir.help"/></td>
    </tr>
  </table>

  <%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>