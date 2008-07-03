<@extends src="base.ftl">
<@block name="title">Welcome to Nuxeo WebEngine!</@block>
<@block name="header"><h1><a href="${appPath}">Nuxeo WebEngine</a></h1></@block>
<@block name="content">
Hello <strong>${Context.principal.name}</strong>! This is the root of your web site.
</p>

<div id="mainContentBox">

<div class="adminBoardSettings">
  <h3> <a href="${basePath}/repository"><img src="/nuxeo/site/files/resources/image/repository.png" width="98" height="98" alt="Repository"><br>Browse Repository</a></h3>
</div>


<!-- bogdan : write the condition here :-) -->
<div class="adminBoardSettings">
  <h3> <a href="${basePath}/admin"><img src="/nuxeo/site/files/resources/image/admin_board.png" width="98" height="98" alt="Repository"><br>Admin board</a></h3>
</div>

<div class="adminBoardSettings">
  <h3> <a href="${basePath}/docs/index.ftl"><img src="/nuxeo/site/files/resources/image/documentation.png" width="98" height="98" alt="Repository"><br>Documentation</a></h3>
</div>

</div>

<div class="tip">
Your web root is <pre>${env.installDir}</pre>
</div>

</@block>

</@extends>
