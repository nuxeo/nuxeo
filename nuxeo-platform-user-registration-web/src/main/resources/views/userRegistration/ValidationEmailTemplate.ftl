<html>

Hello ${registration.userinfo.firstName} ${registration.userinfo.lastName}, <br/>
<br/>
<br/>

Link: <A href="${info['validationBaseURL']}${registration.id}"> access </A> .
 <br/>(${info['validationBaseURL']}${registration.id}).

 <br/><br/>
After that :
<br/><br/>

<p>Login:  ${registration.userinfo.login}</p>
<p>Password: ${registration.userinfo.password}</p>
<p>Document-id: ${registration.docinfo.documentId}</p>
<p>Permission: ${registration.docinfo.permission}</p>
<br/>

<p>Please, update your password after your first login.</p>
<br/>

</html>
