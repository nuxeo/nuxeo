<%@ include file="includes/header.jsp" %>

<h1><fmt:message key="label.userSettings" /></h1>

<%
String directoryType = collector.getConfigurationParam("nuxeo.directory.type");
if (directoryType==null || directoryType.equals("")) {
    directoryType="default";
}
String userGroupStorage = collector.getConfigurationParam("nuxeo.user.group.storage");
if (userGroupStorage==null || userGroupStorage.equals("")) {
    userGroupStorage="default";
}

String directorytyle = "display:none";
String userWarnStyle = "display:block";

if (!directoryType.equals("default")) {
    directorytyle="display:block";
    userWarnStyle = "display:none";
}

%>
<script language="javascript">
function switchDirectory() {
  var value = document.getElementById("directoryTypeSelector").value;
  if (value=='default') {
    $('#directory').css('display','none');
    $('#userWarn').css('display','none');
  } else {
	if (value == 'ldap') {
		$('#userGroupStorageSelector').val('default');
	} else {
		$('#userGroupStorageSelector').val('multiUserGroup');
	}
    $('#directory').css('display','block');
    $('#userWarn').css('display','none');
  }
  $('#refresh').val('true');
  $('#wizardform').submit();
}

function updateDirectorySettings() {
  $('#refresh').val('true');
  $('#wizardform').submit();
}

function checkNetworkSetting() {
  $('#refresh').val('checkNetwork');
  $('#wizardform').submit();
}

function checkAuthSetting() {
  $('#refresh').val('checkAuth');
  $('#wizardform').submit();
}

function checkUserLdapParam() {
  $('#refresh').val('checkUserLdapParam');
  $('#wizardform').submit();
}

function checkGroupLdapParam() {
  $('#refresh').val('checkGroupLdapParam');
  $('#wizardform').submit();
}

$(document).ready(function() {
    $(":checkbox").bind('change', function() {
        if($(this).attr("checked")) {
            $("#" + this.name).val("true");
        }
        else {
            $("#" + this.name).val("false");
        }
    });

    $('#emergencychkbox').bind('change', function() {
        if($(this).attr("checked")) {
            $('#emergencySettings').css('display','table');
        }
        else {
            $('#emergencySettings').css('display','none');
        }
    });
});

</script>


<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
  <fmt:message key="label.userSettings.description" />
  <a href="http://doc.nuxeo.com/x/K4AO" target="doc" href="#">
    <fmt:message key="label.userSettings.doc"/>
  </a>
</span>

<span class="screenExplanations">
<fmt:message key="label.userSettings.explanations" />
</span>

<%@ include file="includes/feedback.jsp" %>
<%
if (ctx.hasInfos()) {%>
    <div class="infoBlock">
    <%
    Map<String,String> infos = ctx.getInfosMap();
    if (infos.containsKey("dn")) { %>
            <div class="infoItem"></div>
            <table style="width:100%">
              <thead>
                <tr><th colspan="2"><fmt:message key="label.nuxeo.ldap.result.found"/> <%= infos.remove("dn") %></th></tr>
              </thead>
              <tfoot>
                <tr><td colspan="2"><fmt:message key="label.nuxeo.ldap.more.result"/><td></tr>
              </tfoot>
              <tbody>
            <% for (String field : infos.keySet()) { %>
                <tr>
                    <td class="title"><%= field %></th>
                    <td><%= infos.get(field).replace(" , ","<br/>") %></td>
                </tr>
            <% } %>
            </tbody>
            </table>
            <div class="errItem"></div>
    <%} else {
        for (String field : infos.keySet()) {%>
  <div class="infoItem" id="info_<%=field%>"><fmt:message key="<%=infos.get(field)%>" /></div>
  <% } %>
       <% } %>
 </div>
<%}%>

<input id="refresh" type='hidden' name="refresh" value=""/>

 <table>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.directory.type"/></td>
      <td>
        <select id="directoryTypeSelector" name="nuxeo.directory.type" onchange="switchDirectory()">
           <option
           <%if ("default".equals(directoryType)){%>
           selected
           <%} %>
           value="default"><fmt:message key="label.nuxeo.directory.default" /></option>

           <option
           <%if ("ldap".equals(directoryType) ){%>
           selected
           <%} %>
           value="ldap"><fmt:message key="label.nuxeo.directory.ldap" /></option>

           <option
           <%if ("multi".equals(directoryType) ){%>
           selected
           <%} %>
           value="multi"><fmt:message key="label.nuxeo.directory.multi" /></option>

        </select>
      </td>
    </tr>
<%if (!"default".equals(directoryType)){%>
    <tr>
       <td class="labelCell"><fmt:message key="label.nuxeo.user.group.storage"/></td>
       <td>
         <select id="userGroupStorageSelector" name="nuxeo.user.group.storage" onchange="updateDirectorySettings()">
   <%if ("ldap".equals(directoryType)){%>
         <option
         <%if ("default".equals(userGroupStorage)){%>
         selected
         <%} %>
         value="default"><fmt:message key="label.directory.storage.default" /></option>

         <option
         <%if ("userLdapOnly".equals(userGroupStorage) ){%>
         selected
         <%} %>
         value="userLdapOnly"><fmt:message key="label.directory.storage.userldap" /></option>
   <% } %>
   <%if ("multi".equals(directoryType)){%>
         <option
         <%if ("multiUserGroup".equals(userGroupStorage)){%>
         selected
         <%} %>
         value="multiUserGroup"><fmt:message key="label.directory.storage.multiUserGroup" /></option>

         <option
         <%if ("multiUserSqlGroup".equals(userGroupStorage) ){%>
         selected
         <%} %>
         value="multiUserSqlGroup"><fmt:message key="label.directory.storage.multiUserSqlGroup" /></option>

         <option
         <%if ("ldapUserMultiGroup".equals(userGroupStorage) ){%>
         selected
         <%} %>
         value="ldapUserMultiGroup"><fmt:message key="label.directory.storage.ldapUserMultiGroup" /></option>
    <% } %>

        </select>
      </td>
    </tr>
    <% } %>
  </table>
 <div id="userWarn" style="<%=userWarnStyle%>" class="warnBlock">
      <fmt:message key="label.userSettings.warning"/>
 </div>

 <div id="directory" style="<%=directorytyle%>">
  <%if (!"default".equals(directoryType)){%>
    <div class="serverconf">
      <h5 id="serverconf">
          <fmt:message key="label.nuxeo.ldap.server.configuration"/>
      </h5>
      <table id="body-serverconf">
        <tr>
          <td class="labelCell">
            <span class="required"><fmt:message key="label.nuxeo.ldap.url"/></span>
          </td>
          <td>
            <input type="text" name="nuxeo.ldap.url" value="<%=collector.getConfigurationParam("nuxeo.ldap.url") %>"
             placeholder="ldap://ldap.testathon.net:389" />
            <a id="checkNetwork" onclick="checkNetworkSetting()" href="#">
              <fmt:message key="label.action.check.network"/>
            </a>
          </td>
        </tr>
        <tr>
          <td class="labelCell">
           <span class="required"><fmt:message key="label.nuxeo.ldap.binddn"/></span>
          </td>
          <td>
           <input type="text" name="nuxeo.ldap.binddn" value="<%=collector.getConfigurationParam("nuxeo.ldap.binddn") %>"
            placeholder="CN=stuart,OU=users,DC=testathon,DC=net" />
           <div class="helpCell"><fmt:message key="label.nuxeo.ldap.binddn.help"/></div>
          </td>
        </tr>
        <tr>
          <td class="labelCell">
            <span class="required"><fmt:message key="label.nuxeo.ldap.bindpassword"/></span>
          </td>
          <td>
            <input type="password" name="nuxeo.ldap.bindpassword" AUTOCOMPLETE="off" value="<%=collector.getConfigurationParam("nuxeo.ldap.bindpassword") %>"
            placeholder="stuart" />
            <a id="checkAuth" onclick="checkAuthSetting()" href="#">
             <fmt:message key="label.action.check.auth"/>
            </a>
          </td>
        </tr>
      </table>
    </div>
    <div class="userdirgeneral">
      <h5 id="userdirgeneral">
          <fmt:message key="label.nuxeo.ldap.user.directory.configuration"/>
      </h5>
      <table id="body-userdirgeneral">
        <tr>
         <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.searchBaseDn"/></span></td>
         <td>
          <input type="text" name="nuxeo.ldap.user.searchBaseDn" placeholder="OU=users,DC=testathon,DC=net"
            value="<%=collector.getConfigurationParam("nuxeo.ldap.user.searchBaseDn") %>"/>
          <div class="helpCell"><fmt:message key="label.nuxeo.ldap.user.searchBaseDn.help"/></div>
          <a id="testUserLdapParam" onclick="checkUserLdapParam()" href="#">
            <fmt:message key="label.action.check.search"/>
          </a>
         </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.user.searchClass"/></td>
          <td>
           <input type="text" name="nuxeo.ldap.user.searchClass" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.searchClass") %>"/>
           <div class="helpCell"><fmt:message key="label.nuxeo.ldap.user.searchClass.help"/></div>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.searchFilter"/></td>
          <td>
           <input type="text" name="nuxeo.ldap.user.searchFilter" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.searchFilter") %>"/>
           <div class="helpCell"><fmt:message key="label.nuxeo.ldap.user.searchFilter.help"/></div>
          </td>
        </tr>
        <tr>
         <td class="labelCell"><fmt:message key="label.nuxeo.ldap.searchScope"/></td>
         <td>
          <select name="nuxeo.ldap.user.searchScope">
           <option
           <%if ("onelevel".equals(collector.getConfigurationParam("nuxeo.ldap.user.searchScope"))){%>
           selected
           <%} %>
           value="onelevel">onelevel</option>

           <option
           <%if ("subtree".equals(collector.getConfigurationParam("nuxeo.ldap.user.searchScope"))){%>
           selected
           <%} %>
           value="subtree">subtree</option>
          </select>
          <div class="helpCell"><fmt:message key="label.nuxeo.ldap.searchScope.help"/></div>
         </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.readonly"/></td>
          <td>
            <input type="checkbox" name="userreadonly" <%=("true".equals(collector.getConfigurationParam("nuxeo.ldap.user.readonly")) ? "checked" : "") %>
              <%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? "disabled=\"disabled\"" : "") %>/>
            <input id="userreadonly" type="hidden" name="nuxeo.ldap.user.readonly"
              value="<%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? collector.getConfigurationParam("nuxeo.ldap.user.readonly") : "true") %>"/>
          </td>
        </tr>
       </table>
    </div>
    <div class="userdirmapping">
      <h5 id="userdirmapping">
          <fmt:message key="label.nuxeo.ldap.fieldMapping"/>
      </h5>
      <div class="helpCell"><fmt:message key="label.nuxeo.ldap.fieldMapping.help"/></div>
      <table id="body-userdirmapping">
        <tr>
          <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.rdn"/></span></td>
          <td>
            <input type="text" name="nuxeo.ldap.user.mapping.rdn" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.rdn") %>"
              placeholder="uid" />
            <div class="helpCell"><fmt:message key="label.nuxeo.ldap.mapping.rdn.help"/></div>
          </td>
        </tr>
        <tr>
          <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.user.mapping.username"/></span></td>
          <td><input type="text" name="nuxeo.ldap.user.mapping.username" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.username") %>"/></td>
        </tr>
        <tr>
          <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.user.mapping.password"/></span></td>
          <td><input type="text" name="nuxeo.ldap.user.mapping.password" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.password") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell required"><fmt:message key="label.nuxeo.ldap.user.mapping.firstname"/></td>
          <td><input type="text" name="nuxeo.ldap.user.mapping.firstname" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.firstname") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell required"><fmt:message key="label.nuxeo.ldap.user.mapping.lastname"/></td>
          <td><input type="text" name="nuxeo.ldap.user.mapping.lastname" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.lastname") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.user.mapping.email"/></td>
          <td><input type="text" name="nuxeo.ldap.user.mapping.email" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.email") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.user.mapping.company"/></td>
          <td><input type="text" name="nuxeo.ldap.user.mapping.company" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.company") %>"/></td>
        </tr>
      </table>
    </div>
    <%if (!"userLdapOnly".equals(userGroupStorage) && !"multiUserSqlGroup".equals(userGroupStorage) ){%>
    <div class="body-groupdir">
      <h5 id="body-groupdir">
          <fmt:message key="label.nuxeo.ldap.group.directory.configuration"/>
      </h5>
      <table id="body-groupdir">
        <tr>
          <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.searchBaseDn"/></span></td>
          <td>
            <input type="text" name="nuxeo.ldap.group.searchBaseDn" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.searchBaseDn") %>"/>
            <div class="helpCell"><fmt:message key="label.nuxeo.ldap.group.searchBaseDn.help"/></div>
            <a id="testGroupLdapParam" onclick="checkGroupLdapParam()" href="#"><fmt:message key="label.action.check.search"/></a>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.searchFilter"/></td>
          <td>
            <input type="text" name="nuxeo.ldap.group.searchFilter" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.searchFilter") %>"/>
            <div class="helpCell"><fmt:message key="label.nuxeo.ldap.group.searchFilter.help"/></div>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.searchScope"/></td>
          <td>
            <select name="nuxeo.ldap.group.searchScope">
               <option
               <%if ("onelevel".equals(collector.getConfigurationParam("nuxeo.ldap.group.searchScope"))){%>
               selected
               <%} %>
               value="onelevel">onelevel</option>

               <option
               <%if ("subtree".equals(collector.getConfigurationParam("nuxeo.ldap.group.searchScope"))){%>
               selected
               <%} %>
               value="subtree">subtree</option>
            </select>
            <div class="helpCell"><fmt:message key="label.nuxeo.ldap.searchScope.help"/></div>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.readonly"/></td>
          <td><input type="checkbox" name="groupreadonly" <%=("true".equals(collector.getConfigurationParam("nuxeo.ldap.group.readonly")) ? "checked" : "") %>
                <%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? "disabled=\"disabled\"" : "") %>/>
              <input id="groupreadonly" type="hidden" name="nuxeo.ldap.group.readonly"
                value="<%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? collector.getConfigurationParam("nuxeo.ldap.group.readonly") : "true") %>"/></td>
        </tr>
      </table>
    </div>
    <div class="fieldmapping">
      <h5 id="fieldmapping">
          <fmt:message key="label.nuxeo.ldap.fieldMapping"/>
      </h5>
      <div class="helpCell"><fmt:message key="label.nuxeo.ldap.fieldMapping.help"/></div>
      <table id="fieldMapping">
        <tr>
          <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.rdn"/></span></td>
          <td><input type="text" name="nuxeo.ldap.group.mapping.rdn" placeholder="cn"
                 value="<%=collector.getConfigurationParam("nuxeo.ldap.group.mapping.rdn") %>" />
              <div class="helpCell"><fmt:message key="label.nuxeo.ldap.mapping.rdn.help"/></div>
          </td>
        </tr>
        <tr>
          <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.group.mapping.name"/></span></td>
          <td><input type="text" name="nuxeo.ldap.group.mapping.name" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.mapping.name") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.group.mapping.label"/></td>
          <td><input type="text" name="nuxeo.ldap.group.mapping.label" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.mapping.label") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.group.mapping.members.staticAttributeId"/></td>
          <td><input type="text" name="nuxeo.ldap.group.mapping.members.staticAttributeId" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.mapping.members.staticAttributeId") %>"/></td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.group.mapping.members.dynamicAttributeId"/></td>
          <td><input type="text" name="nuxeo.ldap.group.mapping.members.dynamicAttributeId" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.mapping.members.dynamicAttributeId") %>"/></td>
        </tr>
      </table>
    </div>
    <%} %>
    <div class="addconf">
      <h5 id="addconf">
          <fmt:message key="label.nuxeo.ldap.additional.configuration"/>
      </h5>
      <table id="body-addconf">
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.defaultAdministratorId"/></td>
          <td><input type="text" name="nuxeo.ldap.defaultAdministratorId" placeholder="jdoe"
          value="<%=collector.getConfigurationParam("nuxeo.ldap.defaultAdministratorId") %>" />
            <div class="helpCell"><fmt:message key="label.nuxeo.ldap.defaultAdministratorId.help"/></div>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.ldap.defaultMembersGroup"/></td>
          <td><input type="text" name="nuxeo.ldap.defaultMembersGroup" placeholder="members"
                value="<%=collector.getConfigurationParam("nuxeo.ldap.defaultMembersGroup") %>" />
            <div class="helpCell"><fmt:message key="label.nuxeo.ldap.defaultMembersGroup.help"/></div>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.user.emergency.enable"/></td>
          <td><input id="emergencychkbox" type="checkbox" name="enableemergency" <%=("true".equals(collector.getConfigurationParam("nuxeo.user.emergency.enable")) ? "checked" : "") %>/>
              <input id="enableemergency" type="hidden" name="nuxeo.user.emergency.enable"
                value="<%=collector.getConfigurationParam("nuxeo.user.emergency.enable") %>"/>
              <div class="helpCell"><fmt:message key="label.nuxeo.user.emergency.enable.help"/></div>
          </td>
        </tr>
        <tr>
          <td></td>
          <td>
            <table id="emergencySettings" style="display:<%=("true".equals(collector.getConfigurationParam("nuxeo.user.emergency.enable")) ? "table" : "none") %>">
              <tr>
                <td><span class="required"><fmt:message key="label.nuxeo.user.emergency.username"/></span></td>
                <td><input type="text" name="nuxeo.user.emergency.username" placeholder=""
                      value="<%=collector.getConfigurationParam("nuxeo.user.emergency.username") %>" /></td>
              </tr>
              <tr>
                <td><span class="required"><fmt:message key="label.nuxeo.user.emergency.password"/></span></td>
                <td><input type="password" name="nuxeo.user.emergency.password" placeholder=""
                      value="<%=collector.getConfigurationParam("nuxeo.user.emergency.password") %>" /></td>
              </tr>
              <tr>
                <td><fmt:message key="label.nuxeo.user.emergency.firstname"/></td>
                <td><input type="text" name="nuxeo.user.emergency.firstname" placeholder=""
                      value="<%=collector.getConfigurationParam("nuxeo.user.emergency.firstname") %>" /></td>
              </tr>
              <tr>
                <td><fmt:message key="label.nuxeo.user.emergency.lastname"/></td>
                <td><input type="text" name="nuxeo.user.emergency.lastname" placeholder=""
                      value="<%=collector.getConfigurationParam("nuxeo.user.emergency.lastname") %>" /></td>
              </tr>
            </table>
          </td>
        </tr>
        <tr>
          <td class="labelCell"><fmt:message key="label.nuxeo.user.anonymous.enable"/></td>
          <td><input type="checkbox" name="enableanonymous" <%=("true".equals(collector.getConfigurationParam("nuxeo.user.anonymous.enable")) ? "checked" : "") %>/>
              <input id="enableanonymous" type="hidden" name="nuxeo.user.anonymous.enable"
                value="<%=collector.getConfigurationParam("nuxeo.user.anonymous.enable") %>"/>
              <div class="helpCell"><fmt:message key="label.nuxeo.user.anonymous.enable.help"/></div>
          </td>
        </tr>
      </table>
    </div>
  <%} %>
</div>

<%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>
