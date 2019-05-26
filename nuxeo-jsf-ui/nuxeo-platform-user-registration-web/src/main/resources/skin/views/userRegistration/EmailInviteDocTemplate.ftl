<html>
<body>
Hello ${userinfo.firstName} ${userinfo.lastName}, <br />
<#if documentTitle != "">
<p>You have been granted permission to access <b>${documentTitle}</b>.</p>
<#else>
<p>You have been granted permission to access <b>${productName}</b>.</p>
</#if>
<#if comment != "">
<br/>
<p>From the sender: </p>
<p>${comment}</p>
</#if>

Username and password are required for connection:<br/>
- username already defined is ${userinfo.login}<br/>
<#if !userAlreadyExists>
- password has to be defined by validating your invitation through this <a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}">link</a>.<br/>
</#if>
<#if userAlreadyExists>
- your usual account password.<br/>
You can validate your invitation through this <a href="${info['enterPasswordUrl']}${configurationName}/${userinfo.id}">link</a>.
</#if>
<#if documentTitle != "">
<p>Once your invitation is validated, you'll be able to log in to the application and access <a href="${info['docUrl']}">${documentTitle}</a>.</p>
<#else>
<p>Once your invitation is validated, you'll be able to log in to the application and access <b>${productName}</b>.</p>
</#if>
The ${productName} Administrators team.
</body>
</html>