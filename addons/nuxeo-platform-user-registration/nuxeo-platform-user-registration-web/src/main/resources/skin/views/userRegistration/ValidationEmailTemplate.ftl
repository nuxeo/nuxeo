<html>
<body>
Hello ${registration.userinfo.firstName} ${registration.userinfo.lastName}, <br />
<br />
You have been invited to access ${registration.docinfo.documentTitle}.
<br />

<p>Click on the following link to validate your invitation:</p>
<br/>
<a href="${info['validationBaseURL']}${registration.id}">Validate my invitation</a>

<br /><br />
<p>Here are your login information:</p>
<p>Login:  ${registration.userinfo.login}</p>
<p>Password:
<#if userAlreadyExists>.
You know your password.
<#else>
${registration.userinfo.password}
</#if>
</p>
<br />

<p>Please, update your password after your first login.</p>
<br />
</body>
</html>
