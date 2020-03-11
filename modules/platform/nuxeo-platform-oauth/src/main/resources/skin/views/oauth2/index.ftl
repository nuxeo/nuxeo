<@extends src="base.ftl">
<@block name="content">

  <#if error>
    <p>${error}</p>
  <#else>
    <p>The token has been registered, you can now access to your service provider.</p>
  </#if>

  <script>
    var data = {
      'token':'${token}'
    };
    window.opener.postMessage(JSON.stringify(data), "*");
    window.close();
  </script>

</@block>
</@extends>