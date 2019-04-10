<html>
<body>
Hello ${userinfo.firstName} ${userinfo.lastName}, <br />
<#if documentTitle != "">
<p>You have been invited to access <a href="${info['docUrl']}">${documentTitle}</a>.</p>
<#else>
<p>You have been invited to access ${productName}.</p>
</#if>

<#if comment != "">
<br/>
<p>From the sender: </p>
<p>${comment}</p>
</#if>

<#if !userAlreadyExists>
<p>Your username is ${userinfo.login} but you need to add a password now. <br />
Click on the following link to validate your invitation by adding your password:</p>
</#if>

<a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}">[Validate my invitation]</a>

<p>Once validated, you'll be able to log in to the application then browse, create and share documents to your collaborators.</p>

<#if userAlreadyExists>
<p>Here are your login credentials:</p>
<p>Username:  ${userinfo.login}</p>
<p>Password: Your usual account password.</p>
</#if>
</p>

<br />
The ${productName} Administrators team.
</body>
</html>
