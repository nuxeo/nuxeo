<%@ include file="includes/header.jsp"%>

<h1>
  <fmt:message key="label.dbSettings" />
</h1>

<%
    String dbTemplate = collector.getConfigurationParam("nuxeo.dbtemplate");
  if (dbTemplate==null || dbTemplate.equals("")) {
    dbTemplate="default";
  }

  String dbSettingStyle = "display:none";
  String dbWarnStyle = "display:block";
  String dbOracleWarnStyle = "display:none";

  if (!dbTemplate.equals("default")) {
    dbSettingStyle="display:block";
    dbWarnStyle = "display:none";
  }
  if (dbTemplate.equals("oracle")) {
    dbOracleWarnStyle = "display:block";
  }

%>
<script language="javascript">
  function updateDBSettings() {
    hidePanels();
    $('#refresh').val('true');
    $('#wizardform').submit();
  }

  function hidePanels() {
    var hideOrDisplay = function (value, test) {
      $("#" + value).css("display", test ? "block" : "none");
    };

    var value = document.getElementById("dbTemplateSelector").value;
    hideOrDisplay("dbSettings", value !== 'default' && value !== "mongodb");
    hideOrDisplay("dbWarn", value === 'default');
    hideOrDisplay("dbOracleWarn", value === "oracle");
    hideOrDisplay("mongoSettings", value === "mongodb");
  }

  $(function() {
    hidePanels();
  });
</script>


<%@ include file="includes/form-start.jsp"%>
<span class="screenDescription"> <fmt:message key="label.dbSettings.description" /> <br />
</span>

<span class="screenExplanations"> <fmt:message key="label.dbSettings.explanations" /> <br />
</span>


<%@ include file="includes/feedback.jsp" %>
<input id="refresh" type='hidden' name="refresh" value="false"/>

<table>
  <tr>
    <td class="labelCell"><fmt:message key="label.nuxeo.dbtemplate"/></td>
    <td>
      <select id="dbTemplateSelector" name="nuxeo.dbtemplate" onchange="updateDBSettings()">
        <optgroup label="<fmt:message key="label.db.relational"/>">
          <option
              <%if ("default".equals(dbTemplate)){%>
              selected
              <%} %>
              value="default"><fmt:message key="label.dbSettings.default" /></option>

          <option
              <%if ("postgresql".equals(dbTemplate) ){%>
              selected
              <%} %>
              value="postgresql">PostgreSQL</option>

          <option
              <%if ("oracle".equals(dbTemplate) ){%>
              selected
              <%} %>
              value="oracle">Oracle</option>

          <option
              <%if ("mssql".equals(dbTemplate) ){%>
              selected
              <%} %>
              value="mssql">MS SQL Server</option>

          <option
              <%if ("mysql".equals(dbTemplate) ){%>
              selected
              <%} %>
              value="mysql">MySQL</option>
        </optgroup>
        <optgroup label="<fmt:message key="label.db.nosql"/>">
          <option
              <%if ("mongodb".equals(dbTemplate) ){%>
              selected
              <%} %>
              value="mongodb">MongoDB</option>
        </optgroup>
      </select>
    </td>
  </tr></table>

<div id="dbWarn" style="<%=dbWarnStyle%>" class="warnBlock">
  <fmt:message key="label.dbSettings.warning"/>
</div>
<div id="dbOracleWarn" style="<%=dbOracleWarnStyle%>" class="warnBlock">
  <fmt:message key="label.dbSettings.oracle.warning"/>
</div>

<div id="mongoSettings" style="<%=dbSettingStyle%>">
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.mongodb.dbname"/></td>
      <td><input type="text" name="nuxeo.mongodb.dbname"
          value="<%=collector.getConfigurationParam("nuxeo.mongodb.dbname") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.mongodb.server"/></td>
      <td><input type="text" name="nuxeo.mongodb.server"
          value="<%=collector.getConfigurationParam("nuxeo.mongodb.server") %>"/></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td class="helpCell">
        <fmt:message key="label.dbSettings.mongodb"/>
        <a href="https://doc.nuxeo.com/x/yAEuAQ" target="doc">
          <fmt:message key="label.dbSettings.doclink"/>
        </a>
      </td>
    </tr>
  </table>
</div>

<div id="dbSettings" style="<%=dbSettingStyle%>">
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.db.name"/></td>
      <td><input type="text" name="nuxeo.db.name" value="<%=collector.getConfigurationParam("nuxeo.db.name") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.db.user"/></td>
      <td><input type="text" name="nuxeo.db.user" AUTOCOMPLETE="off" value="<%=collector.getConfigurationParam("nuxeo.db.user") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.db.password"/></td>
      <td><input type="password" name="nuxeo.db.password" AUTOCOMPLETE="off" value="<%=collector.getConfigurationParam("nuxeo.db.password") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.db.host"/></td>
      <td><input type="text" name="nuxeo.db.host" value="<%=collector.getConfigurationParam("nuxeo.db.host") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.db.port"/></td>
      <td><input type="text" name="nuxeo.db.port" value="<%=collector.getConfigurationParam("nuxeo.db.port") %>" size="5"/></td>
    </tr>
    <tr>
      <td></td>
      <td class="helpCell">
        <fmt:message key="label.dbSettings.doc"/>
        <a href="http://doc.nuxeo.com/x/AYxH" target="doc">
          <fmt:message key="label.dbSettings.doclink"/>
        </a>
      </td>
    </tr>
  </table>
</div>

<%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp"%>
