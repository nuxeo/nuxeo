<html>
<body>
Hello ${registrationDoc.userinfo.firstName} ${registrationDoc.userinfo.lastName}, <br />
<br />

<p>It appears that you did not validate your invitation to ${registrationDoc.docinfo.documentTitle}<p>
<p>Click on the following link to validate your invitation:</p>
<A href="${validationBaseURL}${registrationDoc.id}"> Validate my invitation </A> .
<br />

<br /><br />
<p>Here are your login information:</p>
<p>Login:  ${registrationDoc.userinfo.login}</p>

</body>
</html>
