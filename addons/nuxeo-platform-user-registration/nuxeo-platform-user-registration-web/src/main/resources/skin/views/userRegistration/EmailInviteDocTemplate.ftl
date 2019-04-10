<html>
<body>
Hello ${userinfo.firstName} ${userinfo.lastName}, <br />
<#if documentTitle != "">
<p>You have been granted permission to access <b>${documentTitle}</b>.</p>
<#else>
<p>You have been granted permission to access <b>${productName}</b>.</p>
</#if>
<br />
<#if comment != "">
<br/>
<p>From the sender: </p>
<p>${comment}</p>
</#if>

<p>Username and password are required for connection :</p>
<p>- username already defined is ${userinfo.login}</p>
<#if !userAlreadyExists>
<p>- password has to be defined by validating your invitation through this <a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}">link</a>.</p>
</#if>
<#if userAlreadyExists>
<p>- your usual account password.</p>
<p>You can validate your invitation through this <a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}">link</a>.</p>
</#if>
<br />
<p>Once your invitation is validated, you'll be able to log in to the application and access <a href="${info['docUrl']}">${documentTitle}</a>.</p>
<br />
The ${productName} Administrators team.
</body>
</html>