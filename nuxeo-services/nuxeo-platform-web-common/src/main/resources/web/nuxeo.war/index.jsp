<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page import="org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper"%>
<c:redirect url="<%= LoginScreenHelper.getStartupPageURL(request) %>"/>