<html>
<body>
Hello ${registrationDoc.userinfo.firstName} ${registrationDoc.userinfo.lastName}, <br />
<br />

<p>It appears that you did not validate your invitation to ${registrationDoc.docinfo.documentTitle}.<p>
<#if registrationDoc.registration.comment != "">
<br/>
<p>From the sender: </p>
<p>${registrationDoc.registration.comment}</p>
</#if>

<p>Click on the following link to validate your invitation:</p>
<a href="${info['enterPasswordUrl']}${registrationDoc.id}"> Validate my invitation </a> .
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

</body>
</html>
