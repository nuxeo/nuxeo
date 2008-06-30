<@extends src="base.ftl">
<@block name="title">Welcome to Nuxeo WebEngine!</@block>
<@block name="header"><h1><a href="${appPath}">Nuxeo WebEngine</a></h1></@block>
<@block name="content">
Hello ${Context.principal.name}! This is the root of your web site.
</p>
<p>
Your web root is <pre>${env.installDir}</pre>
</p>
<p>
To browse your repository go <a href="${basePath}/repository">here</a>.
</p>
<p>
See the <a href="${basePath}/docs/index.ftl">manual</a> for more information.
</p>
</@block>

</@extends>
