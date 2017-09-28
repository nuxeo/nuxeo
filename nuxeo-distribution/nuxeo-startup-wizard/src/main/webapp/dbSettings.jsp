<%@page import="org.nuxeo.launcher.config.ConfigurationGenerator"%>
<%@ include file="includes/header.jsp"%>

<h1>
  <fmt:message key="label.dbSettings" />
</h1>

<%
    String dbTemplate = collector.getConfigurationParam(ConfigurationGenerator.PARAM_TEMPLATE_DBNAME);
  if (dbTemplate==null || dbTemplate.equals("")) {
    dbTemplate="default";
  }

  String dbSettingStyle = "display:none";
  String dbWarnStyle = "display:block";
  String dbOracleWarnStyle = "display:none";
  String dbMongoWarnStyle = "display:none";
  String dbMongoSettingsStyle = "display:none";

  if (!dbTemplate.equals("default")) {
    dbSettingStyle="display:block";
    dbWarnStyle = "display:none";
  }
  if (dbTemplate.equals("oracle")) {
    dbOracleWarnStyle = "display:block";
  }
  if (dbTemplate.equals("mongodb")) {
    dbMongoWarnStyle = "display:block";
    dbSettingStyle = "display:none";
    dbMongoSettingsStyle = "display:block";
  }
%>
<script language="javascript">
    function updateDBSettings() {
        hidePanels();
        $('#refresh').val('true');
        $('#wizardform').submit();
    }

    function hidePanels() {
        var hideOrDisplay = function(value, test) {
            $("#" + value).css("display", test ? "block" : "none");
        };

        var dbValue = document.getElementById("dbTemplateSelector").value;
        // handle warns
        hideOrDisplay("dbWarn", dbValue === 'default');
        hideOrDisplay("dbOracleWarn", dbValue === "oracle");
        hideOrDisplay("dbMongoWarn", dbValue === "mongodb");

        // display settings
        hideOrDisplay("dbSettings", dbValue !== 'default' && dbValue !== 'mongodb');
        hideOrDisplay("mongoSettings", dbValue === 'mongodb');
    }

    $(function() {
        hidePanels();
    });
</script>


<%@ include file="includes/form-start.jsp"%>


<span class="screenExplanations">
  <fmt:message key="label.dbSettings.description" /> <br />
  <fmt:message key="label.dbSettings.explanations" />
</span>


<%@ include file="includes/feedback.jsp"%>
<input id="refresh" type='hidden' name="refresh" value="false" />

<div>
  <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.dbtemplate"/></td>
      <td><select id="dbTemplateSelector" name="nuxeo.dbtemplate" onchange="updateDBSettings()">
        <optgroup label="Test">
          <option <%if ("default".equals(dbTemplate)) {%> selected <%}%> value="default"><fmt:message
              key="label.dbSettings.default"/></option>
        </optgroup>
        <optgroup label="SQL">
          <option <%if ("postgresql".equals(dbTemplate)) {%> selected <%}%> value="postgresql">PostgreSQL</option>

          <option <%if ("oracle".equals(dbTemplate)) {%> selected <%}%> value="oracle">Oracle</option>

          <option <%if ("mssql".equals(dbTemplate)) {%> selected <%}%> value="mssql">MS SQL Server</option>

          <option <%if ("mysql".equals(dbTemplate)) {%> selected <%}%> value="mysql">MySQL</option>
        </optgroup>

        <optgroup label="NoSQL">
          <option <%if ("mongodb".equals(dbTemplate)) {%> selected <%}%> value="mongodb">MongoDB</option>
        </optgroup>
      </select></td>
    </tr>
  </table>

  <div id="dbWarn" style="<%=dbWarnStyle%>" class="warnBlock">
    <fmt:message key="label.dbSettings.warning" />
  </div>
  <div id="dbOracleWarn" style="<%=dbOracleWarnStyle%>" class="warnBlock">
    <fmt:message key="label.dbSettings.oracle.warning" />
  </div>
  <div id="dbMongoWarn" style="<%=dbMongoWarnStyle%>" class="warnBlock">
    <fmt:message key="label.dbSettings.mongodb" />
    <a href="https://doc.nuxeo.com/x/yAEuAQ" target="doc">
      <fmt:message key="label.dbSettings.doclink" />
    </a>
  </div>

  <div id="dbSettings" style="<%=dbSettingStyle%>">
    <table>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.db.name" /></td>
        <td><input type="text" name="nuxeo.db.name" value="<%=collector.getConfigurationParam("nuxeo.db.name")%>" /></td>
      </tr>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.db.user" /></td>
        <td><input type="text" name="nuxeo.db.user" AUTOCOMPLETE="off"
          value="<%=collector.getConfigurationParam("nuxeo.db.user")%>" /></td>
      </tr>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.db.password" /></td>
        <td><input type="password" name="nuxeo.db.password" AUTOCOMPLETE="off"
          value="<%=collector.getConfigurationParam("nuxeo.db.password")%>" /></td>
      </tr>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.db.host" /></td>
        <td><input type="text" name="nuxeo.db.host" value="<%=collector.getConfigurationParam("nuxeo.db.host")%>" /></td>
      </tr>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.db.port" /></td>
        <td><input type="text" name="nuxeo.db.port" value="<%=collector.getConfigurationParam("nuxeo.db.port")%>"
          size="5" /></td>
      </tr>
    </table>

  </div>

  <div id="mongoSettings" style="<%=dbSettingStyle%>">
    <table>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.mongodb.dbname" /></td>
        <td><input type="text" name="<%=ConfigurationGenerator.PARAM_MONGODB_NAME %>"
                   value="<%=collector.getConfigurationParam(ConfigurationGenerator.PARAM_MONGODB_NAME)%>" /></td>
      </tr>
      <tr>
        <td class="labelCell"><fmt:message key="label.nuxeo.mongodb.server" /></td>
        <td><input type="text" name="<%=ConfigurationGenerator.PARAM_MONGODB_SERVER %>"
                   value="<%=collector.getConfigurationParam(ConfigurationGenerator.PARAM_MONGODB_SERVER)%>" /></td>
      </tr>
    </table>
  </div>
</div>
<%@ include file="includes/prevnext.jsp"%>

<%@ include file="includes/footer.jsp"%>
