<html>
<body>
Hello ${registrationDoc.userinfo.firstName} ${registrationDoc.userinfo.lastName}, <br />
<br />

<p>It appears that you did not validate your invitation to ${registrationDoc.docinfo.documentTitle}<p>
<p>Click on the following link to validate your invitation:</p>
<A href="${info['validationBaseURL']}${registrationDoc.id}"> Validate my invitation </A> .
<br />

<br /><br />
<p>Here are your login information:</p>
<p>Login:  ${registrationDoc.userinfo.login}</p>
<p>Password:
<#if userAlreadyExists>
Your usual account password.
<#else>
${registrationDoc.userinfo.password}
</#if>
</p>

<#if registrationDoc.registration.comment??>
<p>Comment:</p>
<p>${registrationDoc.registration.comment}</p>
</#if>

</body>
</html>
