
<@extends src="base.ftl">
<@block name="title">Nuxeo WebEngine - Admin board</@block>
<@block name="header"><h1><a href="${Context.modulePath}">Nuxeo WebEngine - Admin board</a></h1></@block>
<@block name="content">

<div id="mainContentBox">

<div class="adminBoardSettings">
  <h3><a href="${basePath}/admin/users"><img src="/nuxeo/site/files/resources/image/admin_users.png" width="98" height="98" alt="User management"><br>User Management</a></h3>
</div>

</div>

<div class="tip">
See the <a href="${basePath}/help">manual</a> for more information.
</div>
</@block>
</@extends>