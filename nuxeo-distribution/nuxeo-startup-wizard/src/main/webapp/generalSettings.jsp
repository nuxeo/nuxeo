<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.generalSettings" /></h1>

<%@ include file="includes/form-start.jsp" %>
<span class="screenExplanations">
  <fmt:message key="label.generalSettings.description" />
  <fmt:message key="label.generalSettings.explanations" />
</span>

<%@ include file="includes/feedback.jsp" %>
  <table>
	<%if (!collector.isSectionSkipped("IP")) { %>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.bind.address"/></td>
      <td><input type="text" name="nuxeo.bind.address" value="<%=collector.getConfigurationParam("nuxeo.bind.address") %>" size="15"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.bind.address.help"/></td>
    </tr>
    <% } %>

    <%if (!collector.isSectionSkipped("Paths")) { %>
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
    <tr>
      <td class="labelCell"><fmt:message key="label.org.nuxeo.dev"/></td>
      <td><input type="checkbox" name="org.nuxeo.dev" value="true" checked /></td>
    </tr><tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.org.nuxeo.dev.help"/></td>
    </tr>
    <% } %>
  </table>

  <%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>
