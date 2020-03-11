<%@ include file="header.jsp" %>

<%
  String serviceId = (String) request.getAttribute("serviceId");
  String token = (String) request.getAttribute("token");
  String service = null;
  if (serviceId.indexOf('?') == -1)
    service = serviceId + "?ticket=" + token;
  else
    service = serviceId + "&ticket=" + token;
  service =
    edu.yale.its.tp.cas.util.StringUtil.substituteAll(service, "\n", "");
  service =
    edu.yale.its.tp.cas.util.StringUtil.substituteAll(service, "\r", "");
  service =
    edu.yale.its.tp.cas.util.StringUtil.substituteAll(service, "\"", "");
%>

            <table width="100%"
                   border="0"
                   cellspacing="0"
                   cellpadding="10"
                   height="100%">
              <tr>
                <td bgcolor="#FFFFFF" valign="top">
                  <font face="Arial, Helvetica, sans-serif"
                        size="4"
                        color="#003399">
                <b>Privacy notice</b></font></td>
              </tr>
              <tr>
                <td bgcolor="#FFFFFF" valign="top" height="363">
                  <p><font face="Arial, Helvetica, sans-serif" size="2">

A service claiming to have the following URL has asked the Central
Authentication Service to log you in:

		  </font></p>

<p>
 <blockquote>
  <b><tt><%= pageContext.findAttribute("serviceId") %></tt></b>
 </blockquote>
</p>

<p>Click <b>Proceed</b> below to proceed.</p>

<p align="center">
  <a href="<%= service %>"><font color="#336699">Proceed</font></a>
</p>

</td>
</tr>
</table>
                </td>
              </tr>
            </table>

<%@ include file="footer.jsp" %>
