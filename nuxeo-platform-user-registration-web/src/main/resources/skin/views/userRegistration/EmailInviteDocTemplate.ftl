<html>
<body>
Hello ${userinfo.firstName} ${userinfo.lastName}, <br />
<br />
<#if documentTitle != "">
You have been invited to access <a href="${info['docUrl']}">${documentTitle}</a>.
<#else>
<p>You have been invited to access to Nuxeo.</p>
</#if>

<br />
<#if comment != "">
<br/>
<p>From the sender: </p>
<p>${comment}</p>
</#if>

<p>Click on the following link to validate your invitation:</p>
<br/>
<a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}">Validate my invitation</a>

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

<br />

Your administrator.
</body>
</html>
