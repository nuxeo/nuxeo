<%@ include file="header.jsp" %>

<p> <b>Vous avez &eacute;t&eacute; d&eacute;connect&eacute; avec succ&egrave;s.</b></p>
<p><b><i><font color="#FF3300"><a href=" <% out.print(request.getParameter("next")); %> ">back</a></font></i></b></p>

<%@ include file="footer.jsp" %>
