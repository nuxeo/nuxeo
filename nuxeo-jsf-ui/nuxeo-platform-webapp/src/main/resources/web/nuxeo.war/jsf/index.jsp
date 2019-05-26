<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page import="org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper"%>
<%
  String redirect = VirtualHostHelper.getBaseURL(request) + "nxstartup.faces";
%>
<c:redirect url="<%=redirect%>"/>
