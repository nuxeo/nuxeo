<%@ include file="includes/header.jsp" %>

<style>
      input[type="text"]
      {
         width: 300px;
      }
      td.foldingBox {
         height: 15px;
         color: black;
         font-weight:bold;
         background-repeat: no-repeat;
         background-color: rgba(148,217,255,0.5);
         background-position:left center;
         padding-left: 20px;
         margin-bottom: 50px;
         cursor: hand;
      }
      .on {
         background-image: url("images/arrow_down.png");
      }
      .off {
         background-image: url("images/arrow_right.png");
      }
      tbody {
         margin-top:30px;
      }
      td.title {
          font-weight:bold;
          vertical-align:top;
          width: 120px;
      }
      
</style>


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
            $('#emergencySettings').css('display','block');
        }
        else {
            $('#emergencySettings').css('display','none');
        }
    });
    
    $("#serverconf").click( function(event) {
        $("#body-"+this.id).toggle("fold");
        $(this).toggleClass("off", 1000);
    } );
    
    $("#userdirgeneral").click( function(event) {
        $("#body-"+this.id).toggle("fold");
        $(this).toggleClass("off", 1000);
    } );
    
    $("#userdirmapping").click( function(event) {
        $("#body-"+this.id).toggle("fold");
        $(this).toggleClass("off", 1000);
    } );
    
    $("#groupdir").click( function(event) {
        $("#body-"+this.id).toggle("fold");
        $(this).toggleClass("off", 1000);
    } );
    $("#addconf").click( function(event) {
        $("#body-"+this.id).toggle("fold");
        $(this).toggleClass("off", 1000);
    } );
});

</script>


<%@ include file="includes/form-start.jsp" %>
<span class="screenDescription">
<fmt:message key="label.userSettings.description" /> <br/>
</span>

<span class="screenExplanations">
<fmt:message key="label.userSettings.explanations" /> <br/>
</span>

<a href="http://doc.nuxeo.com/x/K4AO" target="doc">
    <fmt:message key="label.userSettings.doc"/>
</a>

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
 <div id="userWarn" style="<%=userWarnStyle%>" class="warnBlock">
      <fmt:message key="label.userSettings.warning"/>
 </div>

 <div id="directory" style="<%=directorytyle%>">
  <table><tr><td>
     <table>

    </table>
   <%if (!"default".equals(directoryType)){%>
  <table>
    <tr><td colspan="3" id="serverconf" class="foldingBox on"><fmt:message key="label.nuxeo.ldap.server.configuration"/><th></td>
    <tbody id="body-serverconf">
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.url"/></span></td>
      <td><input type="text" name="nuxeo.ldap.url" value="<%=collector.getConfigurationParam("nuxeo.ldap.url") %>"
             placeholder="ldap://ldap.testathon.net:389" /></td>
      <td>
        <input type="button" class="glossyButton" id="checkNetwork" 
        value="<fmt:message key="label.action.check.network"/>"
        onclick="checkNetworkSetting()"/>
      </td>
    </tr>
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.binddn"/></span></td>
      <td><input type="text" name="nuxeo.ldap.binddn" value="<%=collector.getConfigurationParam("nuxeo.ldap.binddn") %>"
            placeholder="CN=stuart,OU=users,DC=testathon,DC=net" /></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.binddn.help"/></td>
      <td rowspan="2">
        <input type="button" class="glossyButton" id="checkAuth" 
        value="<fmt:message key="label.action.check.auth"/>"
        onclick="checkAuthSetting()"/>
      </td>
    </tr>
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.bindpassword"/></span></td>
      <td><input type="password" name="nuxeo.ldap.bindpassword" AUTOCOMPLETE="off" value="<%=collector.getConfigurationParam("nuxeo.ldap.bindpassword") %>"
          placeholder="stuart" /></td>
    </tr>
    </tbody>
    <tr><td colspan="3" id="userdirgeneral" class="foldingBox on"><fmt:message key="label.nuxeo.ldap.user.directory.configuration"/><th></tr>
    <tbody id="body-userdirgeneral">
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.searchBaseDn"/></span></td>
      <td><input type="text" name="nuxeo.ldap.user.searchBaseDn" placeholder="OU=users,DC=testathon,DC=net" 
            value="<%=collector.getConfigurationParam("nuxeo.ldap.user.searchBaseDn") %>"/></td>
      <td>
        <input type="button" class="glossyButton" id="testUserLdapParam" 
        value="<fmt:message key="label.action.check.search"/>"
        onclick="checkUserLdapParam()"/>
      </td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.user.searchBaseDn.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.user.searchClass"/></td>
      <td><input type="text" name="nuxeo.ldap.user.searchClass" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.searchClass") %>"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.user.searchClass.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.searchFilter"/></td>
      <td><input type="text" name="nuxeo.ldap.user.searchFilter" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.searchFilter") %>"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.user.searchFilter.help"/></td>
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
      </td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.searchScope.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.readonly"/></td>
      <td><input type="checkbox" name="userreadonly" <%=("true".equals(collector.getConfigurationParam("nuxeo.ldap.user.readonly")) ? "checked" : "") %>
            <%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? "disabled=\"disabled\"" : "") %>/>
          <input id="userreadonly" type="hidden" name="nuxeo.ldap.user.readonly" 
            value="<%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? collector.getConfigurationParam("nuxeo.ldap.user.readonly") : "true") %>"/></td>
    </tr>
    </tbody>
    <tr><td colspan="3" id="userdirmapping" class="foldingBox on"><fmt:message key="label.nuxeo.ldap.fieldMapping"/><th></tr>
    <tbody id="body-userdirmapping">
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.fieldMapping.help"/></td>
    </tr>
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.rdn"/></span></td>
      <td><input type="text" name="nuxeo.ldap.user.mapping.rdn" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.rdn") %>"
            placeholder="uid" /></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.mapping.rdn.help"/></td>
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
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.user.mapping.firstname"/></td>
      <td><input type="text" name="nuxeo.ldap.user.mapping.firstname" value="<%=collector.getConfigurationParam("nuxeo.ldap.user.mapping.firstname") %>"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.user.mapping.lastname"/></td>
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
    </tbody>
    <%if (!"userLdapOnly".equals(userGroupStorage) && !"multiUserSqlGroup".equals(userGroupStorage) ){%>
    <tr><td colspan="3" id="groupdir" class="foldingBox on"><fmt:message key="label.nuxeo.ldap.group.directory.configuration"/><th></tr>
    <tbody id="body-groupdir">
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.searchBaseDn"/></span></td>
      <td><input type="text" name="nuxeo.ldap.group.searchBaseDn" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.searchBaseDn") %>"/></td>
      <td>
        <input type="button" class="glossyButton" id="testGroupLdapParam" 
        value="<fmt:message key="label.action.check.search"/>"
        onclick="checkGroupLdapParam()"/>
      </td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.group.searchBaseDn.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.searchFilter"/></td>
      <td><input type="text" name="nuxeo.ldap.group.searchFilter" value="<%=collector.getConfigurationParam("nuxeo.ldap.group.searchFilter") %>"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.group.searchFilter.help"/></td>
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
      </td>      
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.searchScope.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.readonly"/></td>
      <td><input type="checkbox" name="groupreadonly" <%=("true".equals(collector.getConfigurationParam("nuxeo.ldap.group.readonly")) ? "checked" : "") %>
            <%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? "disabled=\"disabled\"" : "") %>/>
          <input id="groupreadonly" type="hidden" name="nuxeo.ldap.group.readonly" 
            value="<%=("multi".equals(collector.getConfigurationParam("nuxeo.directory.type")) ? collector.getConfigurationParam("nuxeo.ldap.group.readonly") : "true") %>"/></td>          
    </tr>
    <tr><th><fmt:message key="label.nuxeo.ldap.fieldMapping"/><th></tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.fieldMapping.help"/></td>
    </tr>
    <tr>
      <td><span class="labelCell required"><fmt:message key="label.nuxeo.ldap.rdn"/></span></td>
      <td><input type="text" name="nuxeo.ldap.group.mapping.rdn" placeholder="cn"
             value="<%=collector.getConfigurationParam("nuxeo.ldap.group.mapping.rdn") %>" /></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.mapping.rdn.help"/></td>
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
    </tbody>
    <%} %>
     <tr><td colspan="3" id="addconf" class="foldingBox on"><fmt:message key="label.nuxeo.ldap.additional.configuration"/><th></tr>
    <tbody id="body-addconf">
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.defaultAdministratorId"/></td>
      <td><input type="text" name="nuxeo.ldap.defaultAdministratorId" placeholder="jdoe"
      value="<%=collector.getConfigurationParam("nuxeo.ldap.defaultAdministratorId") %>" /></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.defaultAdministratorId.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.ldap.defaultMembersGroup"/></td>
      <td><input type="text" name="nuxeo.ldap.defaultMembersGroup" placeholder="members"
            value="<%=collector.getConfigurationParam("nuxeo.ldap.defaultMembersGroup") %>" /></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.ldap.defaultMembersGroup.help"/></td>
    </tr>
    <tr>
      <td class="labelCell"><fmt:message key="label.nuxeo.user.emergency.enable"/></td>
      <td><input id="emergencychkbox" type="checkbox" name="enableemergency" <%=("true".equals(collector.getConfigurationParam("nuxeo.user.emergency.enable")) ? "checked" : "") %>/>
          <input id="enableemergency" type="hidden" name="nuxeo.user.emergency.enable" 
            value="<%=collector.getConfigurationParam("nuxeo.user.emergency.enable") %>"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.user.emergency.enable.help"/></td>
    </tr>
    <tr>
      <td></td>
      <td>
        <table id="emergencySettings" style="display:<%=("true".equals(collector.getConfigurationParam("nuxeo.user.emergency.enable")) ? "block" : "none") %>">
          <tr>
            <td><span class="labelCell required"><fmt:message key="label.nuxeo.user.emergency.username"/></span></td>
            <td><input type="text" name="nuxeo.user.emergency.username" placeholder=""
                  value="<%=collector.getConfigurationParam("nuxeo.user.emergency.username") %>" /></td>
          </tr>
          <tr>
            <td><span class="labelCell required"><fmt:message key="label.nuxeo.user.emergency.password"/></span></td>
            <td><input type="password" name="nuxeo.user.emergency.password" placeholder=""
                  value="<%=collector.getConfigurationParam("nuxeo.user.emergency.password") %>" /></td>
          </tr>
          <tr>
            <td class="labelCell"><fmt:message key="label.nuxeo.user.emergency.firstname"/></td>
            <td><input type="text" name="nuxeo.user.emergency.firstname" placeholder=""
                  value="<%=collector.getConfigurationParam("nuxeo.user.emergency.firstname") %>" /></td>
          </tr>
          <tr>
            <td class="labelCell"><fmt:message key="label.nuxeo.user.emergency.lastname"/></td>
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
            value="<%=collector.getConfigurationParam("nuxeo.user.anonymous.enable") %>"/></td>
    </tr>
    <tr>
      <td colspan="2" class="helpCell"><fmt:message key="label.nuxeo.user.anonymous.enable.help"/></td>
    </tr>
    </tbody>
  </table>
  <%} %>
  </td></tr></table>
  </div>

  <%@ include file="includes/prevnext.jsp" %>

<%@ include file="includes/footer.jsp" %>