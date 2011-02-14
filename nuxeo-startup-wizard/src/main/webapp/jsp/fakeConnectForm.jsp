<%@page import="org.apache.commons.codec.binary.Base64"%><html>

<%
//fake response for testing
StringBuffer sb = new StringBuffer();
sb.append("registrationOK:true\n");
sb.append("CLID:XXXXXXXXTESTXXXXXXXXX-XXXXXXXXXXCLIDXXXXXXXXX\n");
String fakeToken = new String(Base64.encodeBase64(sb.toString().getBytes()));
%>

<h1>This is a Fake Connect form</h1>

The wizard calls this page with a parameter named WizardCB. <br/>

WizardCB = <%=request.getParameter("WizardCB")%><br/>

After registration form is completed, nuxeo.com must issue a redirect (302) to this call back URL.<br/>

It should pass as parameter a token named ConnectRegistrationToken : <br/>

<pre>
&lt;?php
header("Location:<%=request.getParameter("WizardCB")%>?ConnectRegistrationToken=<%=fakeToken%>");
exit();
?&gt;
</pre>

where Token is :
<pre>
Base64 (
registrationOK : true/false \n
CLID : CLID \n
)
</pre>

<A href="<%=request.getParameter("WizardCB")%>?ConnectRegistrationToken=<%=fakeToken%>"> Test </A>

</html>