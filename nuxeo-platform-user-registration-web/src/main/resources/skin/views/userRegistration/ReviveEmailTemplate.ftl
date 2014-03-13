<html>
<body>
Hello ${registrationDoc.userinfo.firstName} ${registrationDoc.userinfo.lastName}, <br />
<br />

<p>It appears that you did not validate your invitation to ${registrationDoc.docinfo.documentTitle}<p>
<p>Click on the following link to validate your invitation:</p>
<A href="${info['enterPasswordUrl']}${registrationDoc.id}"> Validate my invitation </A> .
<br />

<br /><br />
<#if !userAlreadyExists>
<p>After you defined your password, you'll be able to log in to the application.</p>
<p>Your username is: ${registrationDoc.userinfo.login}</p>
</#if>

<#if userAlreadyExists>
<p>Here are your login credentials:</p>
<p>Username:  ${registrationDoc.userinfo.login}</p>
<p>Password: Your usual account password.</p>
</#if>
</p>

<#if registrationDoc.registration.comment != "">
<p>Comment:</p>
<p>${registrationDoc.registration.comment}</p>
</#if>

</body>
</html>
