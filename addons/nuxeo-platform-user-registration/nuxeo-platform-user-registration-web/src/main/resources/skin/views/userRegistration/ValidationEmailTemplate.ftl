<html>
<body>
Hello ${registrationDoc.userinfo.firstName} ${registrationDoc.userinfo.lastName}, <br />
<br />
You have been invited to access ${registrationDoc.docinfo.documentTitle}.
<br />

<p>Click on the following link to validate your invitation:</p>
<br/>
<a href="${info['validationBaseURL']}${registrationDoc.id}">Validate my invitation</a>

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
<br />
<#if registrationDoc.registration.comment??>
<p>Comment:</p>
<p>${registrationDoc.registration.comment}</p>

</#if>
<p>Please, update your password after your first login.</p>
<br />
</body>
</html>
