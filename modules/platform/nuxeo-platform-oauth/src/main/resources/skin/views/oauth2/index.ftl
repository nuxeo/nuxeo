<@extends src="base.ftl">

<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/oauth2.js"></script>
</@block>

<@block name="content">

  <#if error>
    <p>${error}</p>
  <#else>
    <p>The token has been registered, you can now access to your service provider.</p>
  </#if>

  <div data-token="${token}"></div>

</@block>
</@extends>