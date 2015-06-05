<@extends src="base.ftl">
<@block name="content">

  <p>The token has been registered, you can now access to your service provider.</p>

  <script>
    var data = {
      'token':'${token}'
    };
    window.opener.postMessage(JSON.stringify(data), "*");
    window.close();
  </script>

</@block>
</@extends>