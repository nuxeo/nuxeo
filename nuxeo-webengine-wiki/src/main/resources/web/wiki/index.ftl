<@extends src="base.ftl">
<@block name="title">Welcome to Nuxeo WebEngine!</@block>
<@block name="header"><h1><a href="${appPath}">Nuxeo WebEngine</a></h1></@block>
<@block name="content">
Hello <strong>${Context.principal.name}</strong>! This is the root of your web site.
</p>

<div id="mainContentBox">
Welcome to Nuxeo WebEngine. Here are the available applications :
<ul>
  <li><a href="admin">Admin board</a></li>
  <li><a href="wikis">Wikis</a></li>
</ul>

</div>

<div class="tip">
Your web root is <pre>${env.installDir}</pre>
</div>

</@block>

</@extends>
