<%@page import="org.apache.commons.codec.binary.Base64"%><html>
<head>
<style>
html, body, div, span, applet, object, iframe, h1, h2, h3, h4, h5, h6, p, blockquote, pre, a, abbr, acronym, address, big, cite, code, del, dfn, em, font, img, ins, kbd, q, s, samp, small, strike, strong, sub, sup, tt, var, dl, dt, dd, ol, ul, li, fieldset, form, label, legend, table, caption, tbody, tfoot, thead, tr, th, td {
  border:0 none;
  font:normal 12px "Lucida Grande", Verdana, sans-serif;
  margin:0;
  padding:0;
  color:#664e4e;
}

h1 {
  font:bold 22px "Lucida Grande", Verdana, sans-serif;
  margin:10px 0 15px;
  color:#000;
  text-shadow:0 1px 0 #FFFFFF;
}

.glossyButton, a.glossyButton {
  background-color:#227ce6;
  border:1px solid #227ce6;
  padding: 5px 20px 7px;
  color: #f1f5f7;
  text-decoration: none;
  -moz-border-radius: 6px;
  -webkit-border-radius: 6px;
  -moz-box-shadow: 0 1px 3px rgba(0,0,0,0.6);
  -webkit-box-shadow: 0 1px 3px rgba(0,0,0,0.6);
  text-shadow: 0 -1px 1px rgba(0,0,0,0.25);
  font-weight:bold;
  font-size:15px;
  position: relative;
  cursor: pointer;
  }

.glossyButton:hover, a.glossyButton:hover {
  background-color: #2888f8;
  border:1px solid #207ae4;
  color:#fff;
  }

.formPadding {
  height:290px;
  border-style:solid;
  border-width:0px;
 }
</style>

</head>
<%
//fake response for testing
StringBuffer sb = new StringBuffer();
sb.append("registrationOK:true\n");
sb.append("CLID:XXXXXXXXTESTXXXXXXXXX-XXXXXXXXXXCLIDXXXXXXXXX\n");
String fakeToken = new String(Base64.encodeBase64(sb.toString().getBytes()));
%>

<h1>This is a Fake Connect form</h1>

<div class="formPadding">

<br/>
<center>
<b>
 ---- Here goes Connect Registration Form from Connect Web Site ---
 </b>
</center>

<!--
The wizard calls this page with a parameter named WizardCB. <br/>

WizardCB = <%=request.getParameter("WizardCB")%><br/>

After registration form is completed, nuxeo.com must issue a redirect (302) to this call back URL.<br/>

It should pass as parameter a token named ConnectRegistrationToken : <br/>

<pre>
&lt;?php
header("Location:<%=request.getParameter("WizardCB")%>&ConnectRegistrationToken=<%=fakeToken%>");
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
 -->
 <script>
 function nav(target) {
   var url='<%=request.getParameter("WizardCB")%>&ConnectRegistrationToken=<%=fakeToken%>&action=' + target;
   window.location.href=url;
 }
 </script>
</div>
<center>
<input type="button" class="glossyButton" onclick="nav('prev')" value="Previous"/>
<input type="button" class="glossyButton" onclick="nav('register')" value="Validate"/>
<input type="button" class="glossyButton" onclick="nav('skip')" value="Skip"/>
</center>
</html>