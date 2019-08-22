<html>
<body>
Hello ${userinfo.firstName} ${userinfo.lastName}, <br />
<br />
<#if documentTitle != "">
<p>It appears that you did not validate your invitation to ${documentTitle}.<p>
<#else>
<p>It appears that you did not validate your invitation.</p>
</#if>
<#if comment != "">
<br/>
<p>From the sender: </p>
<p>${comment}</p>
</#if>

<p>Click on the following link to validate your invitation:</p>
<a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}"> Validate my invitation </a> .
<br />

<br /><br />
<#if !userAlreadyExists>
<p>After you defined your password, you'll be able to log in to the application.</p>
<p>Your username is: ${userinfo.login}</p>
</#if>

<#if userAlreadyExists>
<p>Here are your login credentials:</p>
<p>Username:  ${userinfo.login}</p>
<p>Password: Your usual account password.</p>
</#if>
</p>

</body>
</html>
