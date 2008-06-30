<html>
<head>
  <title>Nuxeo WebEngine</title>
</head>
<body>
<h1>Nuxeo WebEngine</h1>
<p>
Hello ${Context.principal.name}! This is the root of your web site.
</p>
<p>
Your web root is <pre>${env.installDir}</pre>
</p>
<p>
To setup your web application go <a href="${basePath}/admin">here</a>.
</p>
<p>
See the <a href="${basePath}/docs/index.ftl">manual</a> for more information.
</p>
<hr>
<#if Context.principal.name == "Guest">
  <#include "common/login.ftl"/>
<#else>
  <a href="${basePath}?nuxeo_login=true">Logout</a>
</#if>
</body>
</html>
