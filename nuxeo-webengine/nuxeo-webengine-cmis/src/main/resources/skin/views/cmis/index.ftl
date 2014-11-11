<@extends src="base.ftl">
  <@block name="title">Index</@block>
  <@block name="header">
    Beginning with Webengine !
  </@block>
  <@block name="content">
    This is the <i>index</i> of your module.
    <div>
      <a href="${This.path}/index1">Navigate to Index1 page</a>
    </div>

  </@block>
  <@block name="footer">
    <div class="tip">
      Your web root is <pre>${env.installDir}</pre>
    </div>
  </@block>
</@extends>
