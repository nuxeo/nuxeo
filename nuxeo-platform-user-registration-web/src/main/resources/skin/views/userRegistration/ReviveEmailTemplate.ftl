<html>
<body>
Hello ${registrationDoc.userinfo.firstName} ${registrationDoc.userinfo.lastName}, <br />
<br />
<br />

Link: <A href="${validationBaseURL}${registrationDoc.id}"> access </A> .
<br />(${validationBaseURL}${registrationDoc.id}).

<br /><br />
After that :
<br /><br />

<p>Login:  ${registrationDoc.userinfo.login}</p>

<p>Password: ${registrationDoc.userinfo.password}</p>

<p>Please, update your password after your first login.</p>
<br />
</body>
</html>
